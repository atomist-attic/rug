package com.atomist.rug

import com.atomist.util.scalaparsing.SimpleLiteral
import com.atomist.project.SimpleProjectOperationArguments
import com.atomist.project.edit._
import com.atomist.rug.InterpreterRugPipeline.DefaultRugArchive
import com.atomist.rug.kind.DefaultTypeRegistry
import com.atomist.rug.kind.java.JavaTypeUsageTest
import com.atomist.rug.parser.{RunOtherOperation, WrappedFunctionArg}
import com.atomist.rug.runtime.rugdsl.RugDrivenProjectEditor
import com.atomist.source.{EmptyArtifactSource, SimpleFileBasedArtifactSource, StringFileArtifact}
import org.scalatest.{FlatSpec, Matchers}

class UsesTest extends FlatSpec with Matchers {

  val simpleAs = new SimpleFileBasedArtifactSource("name",
    Seq(
      StringFileArtifact("filename", "some content")
    )
  )

  it should "not allow using unknown editor" in {
    val prog =
      """
        |@description "Update Kube spec to redeploy a service"
        |editor Redeploy
        |
        |with File f
        | when { f.name().contains("80-deployment") };
        |do
        |  replace ".*" "foo";
        |
        |DoesNotExist
      """.stripMargin
    an[UndefinedRugUsesException] should be thrownBy {
      create(prog)
    }
  }

  it should "not allow importing unused editor" in {
    val prog =
      """
        |@description "Update Kube spec to redeploy a service"
        |editor Redeploy
        |
        |# The problem is that we DON'T actually use this editor
        |uses DoesExist
        |
        |with File f
        | when { f.name().contains("80-deployment") };
        |do
        |  replace ".*" "foo";
        |
        |
        |
        |editor DoesExist
        |
        |with File f do append "foobar"
      """.stripMargin
    an[UnusedUsesException] should be thrownBy {
      create(prog)
    }
  }

  it should "use known editor in same file" in {
    val prog =
      """
        |editor Redeploy
        |
        |with File f
        |do
        |  replace "some" "foo"
        |
        |Foo
        |
        |editor Foo
        |
        |with File f
        |do replace "content" "bar"
      """.stripMargin
    val pe = create(prog).find(_.name.equals("Redeploy")).get
    pe.modify(simpleAs, SimpleProjectOperationArguments.Empty)
    match {
      case sm: SuccessfulModification =>
        sm.result.totalFileCount should be(1)
        // Check that both editors ran
        sm.result.findFile("filename").get.content should equal("foo bar")
    }
  }

  it should "use known editor in same file with explicit namespace" in {
    val prog =
      """
        |editor Redeploy
        |
        |with File f
        |do
        |  replace "some" "foo"
        |
        |Foo
        |
        |editor Foo
        |
        |with File f
        |do replace "content" "bar"
      """.stripMargin
    val namespace = "com.foobar"
    val pe = create(prog, Some(namespace)).find(_.name.equals(namespace + ".Redeploy")).get
    pe.name should equal(namespace + ".Redeploy")
    pe.modify(simpleAs, SimpleProjectOperationArguments.Empty) match {
      case sm: SuccessfulModification =>
        sm.result.totalFileCount should be(1)
        // Check that both editors ran
        sm.result.findFile("filename").get.content should equal("foo bar")
    }
  }

  it should "use known editors in same file with name overlap" in {
    val prog =
      """
        |editor Redeploy
        |
        |with File f
        |do
        |  replace "some" "foo"
        |
        |Foo
        |Foo2
        |
        |editor Foo
        |
        |with File f
        |do replace "content" "bar"
        |
        |editor Foo2
        |
        |with File
        |do replace "bar" "baz"
      """.stripMargin
    val pe = create(prog).find(_.name.equals("Redeploy")).get
    pe.modify(simpleAs, SimpleProjectOperationArguments.Empty)
    match {
      case sm: SuccessfulModification =>
        sm.result.totalFileCount should be(1)
        // Check that both editors ran
        sm.result.findFile("filename").get.content should equal("foo baz")
    }
  }

  it should "call other specified namespace from namespace" in {
    val prog =
      """
        |editor Redeploy
        |
        |uses foo.Foo
        |
        |with File f
        |do
        |  replace "some" "foo"
        |
        |Foo
      """.stripMargin
    val global =
      """
        |editor Foo
        |
        |with File f
        |do replace "content" "bar"
      """.stripMargin
    val namespace = "com.foobar"
    val foo = create(global, Some("foo")).head
    val pe = create(prog, Some(namespace), Seq(foo)).head
    pe.name should equal(namespace + ".Redeploy")
    pe.modify(simpleAs, SimpleProjectOperationArguments.Empty) match {
      case sm: SuccessfulModification =>
        sm.result.totalFileCount should be(1)
        // Check that both editors ran
        sm.result.findFile("filename").get.content should equal("foo bar")
    }
  }

  it should "export no parameters from used editor without parameters" in {
    val prog =
      """
        |editor Redeploy
        |
        |with File f
        |do
        |  replace "some" "foo"
        |
        |editor Foo
        |
        |with File f
        |do replace "some" "bar"
      """.stripMargin
    val ed = create(prog).find(_.name.equals("Redeploy")).get
    ed.parameters should equal(Nil)
  }

  it should "export single parameters from used editor" in {
    val prog =
      """
        |editor Redeploy
        |
        |with File f
        |do
        |  replace "some" "foo"
        |
        |Foo
        |
        |editor Foo
        |
        |param foo: ^.*$
        |
        |with File f
        |do replace "some" "bar"
      """.stripMargin
    val ed = create(prog).find(_.name.equals("Redeploy")).get
    val red = ed.asInstanceOf[RugDrivenProjectEditor]
    red.program.runs should equal(Seq(RunOtherOperation("Foo", Nil, None, None, None)))
    ed.parameters.size should be(1)
    ed.parameters.head.getName should be("foo")
  }

  it should "export all extra parameters from imported editor minus ones that are set" in
    exportAllExtraParametersFromImportedEditorMinusOnesThatAreSet(None)

  it should "export all extra parameters from imported editor minus ones that are set, in namespace" in
    exportAllExtraParametersFromImportedEditorMinusOnesThatAreSet(Some("foo.bar"))

  private def exportAllExtraParametersFromImportedEditorMinusOnesThatAreSet(namespace: Option[String]) = {
    val prog =
      """
        |editor Redeploy
        |
        |param foo: ^.*$
        |
        |with File f
        |do
        |  replace "some" "foo"
        |Foo
        |
        |editor Foo
        |
        |param foo: ^.*$
        |param bar: ^.*$
        |
        |with File f
        | do replace "some" "bar"
      """.stripMargin
    val ed = create(prog, namespace).find(_.name.equals(namespace.map(ns => ns + ".").getOrElse("") + "Redeploy")).get
    ed.parameters.size should be(2)
    ed.parameters.map(p => p.name).toSet should equal(Set("foo", "bar"))
  }

  it should "not export duplicate parameters" in {
    val prog =
      """
        |editor Redeploy
        |
        |param foo: ^.*$
        |
        |with File f
        |do
        |  replace "some" "foo"
        |Foo
        |Bar
        |
        |editor Foo
        |
        |param foo: ^.*$
        |param bar: ^.*$
        |
        |with File f
        | do replace "some" "bar"
        |
        |editor Bar
        |
        |param foo: ^.*$
        |param bar: ^.*$
        |
        |with File f
        | do replace "some" "bar"
      """.stripMargin
    val ed = create(prog, None).find(_.name.equals("Redeploy")).get
    ed.parameters.size should be(2)
    ed.parameters.map(p => p.name).toSet should equal(Set("foo", "bar"))
  }

  it should "preserve parameter ordering per order of editors" in {
    val prog =
      """
        |editor Redeploy
        |
        |with File f
        |do
        |  replace "some" "foo"
        |Foo
        |Bar
        |
        |editor Foo
        |
        |param foo: ^.*$
        |param bar: ^.*$
        |
        |with File f
        | do replace "some" "bar"
        |
        |editor Bar
        |
        |param baz: ^.*$
        |
        |with File f
        | do replace "some" "bar"
      """.stripMargin
    val ed = create(prog, None).find(_.name.equals("Redeploy")).get
    ed.parameters.map(p => p.name).toList should equal(List("foo", "bar", "baz"))
    val prog2 =
      """
        |editor Redeploy
        |
        |with File f
        |do
        |  replace "some" "foo"
        |Bar
        |Foo
        |
        |editor Foo
        |
        |param foo: @any
        |param bar: @any
        |
        |with File f
        | do replace "some" "bar"
        |
        |editor Bar
        |
        |param baz: @any
        |
        |with File f
        | do replace "some" "bar"
      """.stripMargin
    val ed2 = create(prog2, None).find(_.name.equals("Redeploy")).get
    ed2.parameters.map(p => p.name).toList should equal(List("baz", "foo", "bar"))
  }

  it should "export for otherwise empty using editor" in
    exportForOtherwiseEmpty(None)

  it should "export for otherwise empty using namespace" in
    exportForOtherwiseEmpty(Some("whatever"))

  private def exportForOtherwiseEmpty(namespace: Option[String]) {
    val prog =
      """
        |editor PackageMove
        |
        |param old_package: @java_package
        |param new_package: @java_package
        |
        |with JavaSource j when pkg = old_package
        |	do movePackage to new_package
        |
        |editor ParameterizePackage
        |
        |let old_package = "com.atomist.springrest"
        |
        |PackageMove
      """.stripMargin
    val ed = create(prog, namespace).find(_.name.equals(namespace.map(ns => ns + ".").getOrElse("") + "ParameterizePackage")).get
    ed.parameters.size should be(1)
    val red = ed.asInstanceOf[RugDrivenProjectEditor]
    red.program.runs should equal(Seq(RunOtherOperation("PackageMove", Nil, None, None, None)))
    red.program.computations.size should be(1)
    red.program.withs.size should be(0)
    ed.parameters.map(p => p.name).toSet should equal(Set("new_package"))
    // Check it works OK with these parameters
    ed.modify(new EmptyArtifactSource(""), SimpleProjectOperationArguments("", Map[String, String]("new_package" -> "test"))) match {
      case nmn: NoModificationNeeded =>
    }
    val as = new SimpleFileBasedArtifactSource("", StringFileArtifact("src/main/java/com/atomist/springrest/Dog.java",
      """
        |package com.atomist.springrest;
        |class Dog {}
      """.stripMargin))
    ed.modify(as, SimpleProjectOperationArguments("", Map[String, String]("new_package" -> "com.foo"))) match {
      case sm: SuccessfulModification =>
    }
  }

  it should "allow local arguments to be provided explicitly to run" in {
    val prog =
      """
        |editor PackageMove
        |
        |param old_package: @java_package
        |param new_package: @java_package
        |
        |with JavaSource j when pkg = old_package
        |	do movePackage to new_package
        |
        |editor ParameterizePackage
        |
        | PackageMove old_package = "com.atomist.test1", new_package = "com.foo.bar"
      """.stripMargin
    val ed = create(prog, None).find(_.name.equals("ParameterizePackage")).get
    ed.parameters.size should be(0)
    val red = ed.asInstanceOf[RugDrivenProjectEditor]
    red.program.runs should equal(Seq(RunOtherOperation("PackageMove", Seq(
      WrappedFunctionArg(SimpleLiteral("com.atomist.test1"), parameterName = Some("old_package")),
      WrappedFunctionArg(SimpleLiteral("com.foo.bar"), parameterName = Some("new_package"))
    ), None, None, None)))
    red.program.withs.size should be(0)
    ed.modify(JavaTypeUsageTest.NewSpringBootProject, SimpleProjectOperationArguments("", Map[String, String]())) match {
      case sm: SuccessfulModification =>
    }
  }

  it should "reject bogus arguments provided explicitly to run" in {
    val gloriouslyBogusName = "what_in_gods_holy_name_are_you_blathering_about"
    val prog =
      """
        |editor PackageMove
        |
        |param old_package: @java_package
        |param new_package: @java_package
        |
        |with JavaSource j when pkg = old_package
        |	do movePackage to new_package
        |
        |editor ParameterizePackage
        |
        |PackageMove """.stripMargin +
        gloriouslyBogusName +
        """ = "com.atomist.test1", new_package = "com.foo.bar"
        """.stripMargin
    try {
      create(prog, None).find(_.name.equals("ParameterizePackage")).get
      fail(s"Should have rejected bogus local argument to run")
    }
    catch {
      case rrt: RugRuntimeException =>
        rrt.getMessage.contains(gloriouslyBogusName)
    }
  }

  it should "identify type incompatibilities between declared parameters" is pending

  it should "not export extra parameter from imported operation when one is computed" in {
    val prog =
      """
        |editor Redeploy
        |
        |let bar = "abc"
        |
        |with File f
        |do
        |  replace "some" "foo"
        |Foo
        |
        |editor Foo
        |
        |param foo: @semantic_version
        |param bar: @version_range
        |
        |with File f
        | do replace "some" "bar"
      """.stripMargin
    val ed = create(prog).find(_.name.equals("Redeploy")).get
    ed.parameters.size should be(1)
    ed.parameters.map(p => p.name).toSet should equal(Set("foo"))
  }

  it should "validate setting used operation parameters via compute" is pending

  private def create(prog: String, namespace: Option[String] = None, globals: Seq[ProjectEditor] = Nil): Seq[ProjectEditor] = {
    val runtime = new DefaultRugPipeline(DefaultTypeRegistry)
    val rugAs = new SimpleFileBasedArtifactSource(DefaultRugArchive, StringFileArtifact(runtime.defaultFilenameFor(prog), prog))
    runtime.create(rugAs,namespace,globals).asInstanceOf[Seq[ProjectEditor]]
  }
}
