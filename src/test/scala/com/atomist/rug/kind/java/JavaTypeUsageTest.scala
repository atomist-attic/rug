package com.atomist.rug.kind.java

import com.atomist.project.SimpleProjectOperationArguments
import com.atomist.project.edit.{ModificationAttempt, NoModificationNeeded, ProjectEditor, SuccessfulModification}
import com.atomist.rug._
import com.atomist.rug.compiler.typescript.TypeScriptCompiler
import com.atomist.rug.compiler.typescript.compilation.CompilerFactory
import com.atomist.rug.kind.DefaultTypeRegistry
import com.atomist.rug.kind.java.JavaVerifier._
import com.atomist.source._
import com.atomist.source.file.ClassPathArtifactSource
import com.typesafe.scalalogging.LazyLogging
import org.scalatest.{FlatSpec, Matchers}

object JavaTypeUsageTest extends Matchers {

  val NewSpringBootProject: ArtifactSource =
    ClassPathArtifactSource.toArtifactSource("./springboot1")

  val JavaAndText: ArtifactSource = new SimpleFileBasedArtifactSource("name",
    Seq(
      StringFileArtifact("pom.xml", "<maven></maven"),
      StringFileArtifact("/src/main/java/Dog.java",
        """
          |import java.util.Set;
          |import com.foo.Bar;
          |import com.someone.ComFooBar;
          |import com.someone.FooBar;
          |
          |@Bar
          |class Dog {
          |
          |   @ComFooBar
          |   private String stringField;
          |
          |   public Dog() {}
          |
          |   public Dog(String stringField) {
          |     this.stringField = stringField;
          |   }
          |
          |   @FooBar
          |   public void bark() {
          |   }
          |}""".stripMargin)
    )
  )

  def executeJava(program: String, rugPath: String, as: ArtifactSource = JavaAndText): ArtifactSource = {
    val r = attemptToModify(program, rugPath, as, Map[String, String]())
    r match {
      case sm: SuccessfulModification =>
        verifyJavaIsWellFormed(sm.result)
        sm.result
    }
  }

  def attemptToModify(program: String,
                      rugPath: String,
                      as: ArtifactSource, poa: Map[String,String],
                      runtime : RugPipeline = new DefaultRugPipeline(DefaultTypeRegistry)): ModificationAttempt = {


    val progAs = new SimpleFileBasedArtifactSource("", StringFileArtifact(rugPath, program)).withPathAbove(".atomist") + TestUtils.user_model

    val eds = runtime.create(progAs,None)

    val pe = eds.head.asInstanceOf[ProjectEditor]
    pe.modify(as, SimpleProjectOperationArguments("", poa))
  }
}

class JavaTypeUsageTest extends FlatSpec with Matchers with LazyLogging {

  import JavaTypeUsageTest._

  private val tsPipeline = new CompilerChainPipeline(Seq(new TypeScriptCompiler(CompilerFactory.create())))

  it should "find boot package using let and rug" in {
    val program =
      """
        |editor PackageFinder
        |
        |let spb = $(/SpringBootProject())
        |
        |with spb p do eval { print("appPackage=" + p.applicationClassPackage()) }
      """.stripMargin

    attemptToModify(program, "editors/PackageFinder.rug", NewSpringBootProject, Map()) match {
      case nmn: NoModificationNeeded => // Ok
    }
  }

  it should "find boot package using let and typescript" in {
    val program =
      """
        |import {ProjectEditor} from "@atomist/rug/operations/ProjectEditor"
        |import {Project,SpringBootProject} from '@atomist/rug/model/Core'
        |import {Match,PathExpression,PathExpressionEngine,TreeNode} from '@atomist/rug/tree/PathExpression'
        |
        |class PackageFinder implements ProjectEditor {
        |    name: string = "package.finder"
        |    description: string = "Find a spring boot package"
        |    edit(project: Project) {
        |      let eng: PathExpressionEngine = project.context().pathExpressionEngine();
        |      let pe = new PathExpression<Project,SpringBootProject>(`/SpringBootProject()`)
        |      let p = eng.scalar(project, pe)
        |    }
        |}
        |
        |var finder = new PackageFinder()
      """.stripMargin

    attemptToModify(program, "editors/PackageFinder.ts", NewSpringBootProject, Map(), runtime = tsPipeline) match {
      case nmn: NoModificationNeeded => // Ok
    }
  }

  // TODO need to move to path expressions
//  it should "allow project root view" in {
//    val program =
//      """
//        |@description "I add Foobar annotations"
//        |editor ClassAnnotated
//        |
//        |with java.project p when { p.fileCount() > 1 }
//        | with JavaSource j when typeCount = 1
//        |   with JavaType c when true
//        |     do
//        |      addAnnotation "com.foo" "FooBar"
//      """.stripMargin
//    annotateClass(program)
//  }

  it should "annotate class using JavaScript" in {
    val program =
      """
        |@description "I add FooBar annotations"
        |editor ClassAnnotated
        |
        |# with java.project p when { p.fileCount() > 1 }
        |with JavaSource j when typeCount = 1
        |with JavaType c when true
        |do
        |  eval {
        |   // print(c);
        |   return c.addAnnotation("com.foo", "FooBar")
        | };
      """.stripMargin

    annotateClass(program)
  }

  it should "annotate class using path in predicate" in pendingUntilFixed {
    val program =
      """
        |@description "I add FooBar annotations"
        |editor ClassAnnotated
        |
        |# with java.project p when { p.fileCount() > 1 }
        |with JavaSource j when path.startsWith "src/main/java"
        |with JavaType c when true
        |do
        |  eval {
        |   // print(c);
        |   return c.addAnnotation("com.foo", "FooBar")
        | };
      """.stripMargin
    annotateClass(program)
  }

  it should "annotate class using default predicates" in {
    val program =
      """
        |@description "I add FooBar annotations"
        |editor ClassAnnotated
        |
        |with JavaSource
        | with JavaType
        |   do
        |      addAnnotation "com.foo" "FooBar"
      """.stripMargin

    annotateClass(program)
  }

  it should "annotate class using function" in {
    val program =
      """
        |@description "I add FooBar annotations"
        |editor ClassAnnotated
        |
        |with JavaSource j
        |with JavaType c
        |do
        |  addAnnotation "com.foo" "FooBar"
      """.stripMargin

    annotateClass(program)
  }

  it should "annotate class using function with JavaScript argument" in {
    val program =
      """
        |@description "I add FooBar annotations"
        |editor ClassAnnotated
        |
        |with JavaSource j when { j.lineCount() < 1000 }
        |with JavaType c when { c.lineCount() < 100 }
        |do
        |  addAnnotation { "com.foo" } "FooBar"
      """.stripMargin

    annotateClass(program)
  }

  it should "annotate class going straight to class without enclosing JavaSource" in {
    val program =
      """
        |@description "I add FooBar annotations"
        |editor ClassAnnotated
        |
        |with JavaType c
        |do
        |  addAnnotation { "com.foo" } "FooBar"
      """.stripMargin

    annotateClass(program)
  }

  it should "add an annotation with properties to class" in {
    val program =
      """
        |@description "I add ExtendWith() annotations"
        |editor ClassAnnotated
        |
        |with JavaType c
        |begin
        | do addAnnotation "org.junit.jupiter.api.extension" "ExtendWith(value = SpringExtension.class)"
        |end
      """.stripMargin

    val result = executeJava(program, "editors/ClassAnnotated.rug")
    val f = result.findFile("src/main/java/Dog.java").get

    f.content.lines.size should be > 0
    f.content should include("import org.junit.jupiter.api.extension.ExtendWith;")
    f.content should include("@ExtendWith(value = SpringExtension.class)")
  }

  it should "remove annotation from class" in {
    val program =
      """
        |@description "I add Bar annotations"
        |editor ClassAnnotated
        |
        |with JavaType c
        |begin
        | do removeAnnotation { "com.foo" } "Bar"
        |end
      """.stripMargin

    val result = executeJava(program, "editors/ClassAnnotated.rug")
    val f = result.findFile("src/main/java/Dog.java").get

    f.content.lines.size should be > 0
    f.content should include("import com.foo.Bar;")
    f.content shouldNot include("@Bar")
  }

  val dog = StringFileArtifact(
    "src/main/java/com/foo/bar/Dog.java",
    """
      |package com.foo.bar;
      |class Dog {}
    """.stripMargin
  )

  val cat = StringFileArtifact(
    "src/main/java/com/foo/bar/Cat.java",
    """
      |package com.foo;
      |import com.foo.bar.Dog;
      |class Cat {
      | // Doesn't need to be refactored
      | private Dog dog = null;
      |
      |}
    """.stripMargin
  )

  val squirrel = StringFileArtifact(
    "src/main/java/com/foo/Squirrel.java",
    """
      |package com.foo;
      |class Squirrel {
      | // Needs to be refactored
      | private Dog dog = null;
      |
      |}
    """.stripMargin
  )

  it should "repackage class and verify name and path" in {
    val program =
      """
        |@description "I add FooBar annotations"
        |editor ClassAnnotated
        |
        |with JavaType c
        |do
        |  movePackage to "com.atomist"
      """.stripMargin

    val as = new SimpleFileBasedArtifactSource("", dog)
    val result = executeJava(program, "editors/ClassAnnotated.rug", as)
    result.allFiles.foreach(f =>
      logger.debug(f.path + "\n" + f.content + "\n"))

    val f = result.findFile("src/main/java/com/atomist/Dog.java").get
    result.findFile(dog.path).isDefined should be(false)
    f.content should include("package com.atomist;")
  }

  it should "repackage class and verify explicitly importing users are updated" in pendingUntilFixed {
    val program =
      """
        |@description "I add FooBar annotations"
        |editor ClassAnnotated
        |
        |with JavaType c when name = "Dog"
        |do
        |  movePackage to "com.atomist"
      """.stripMargin

    val as = new SimpleFileBasedArtifactSource("", Seq(dog, cat, squirrel))

    val result = executeJava(program,"editors/ClassAnnotated.rug",  as)
    result.allFiles.foreach(f => logger.debug(f.path + "\n" + f.content + "\n"))

    val f = result.findFile("src/main/java/com/atomist/Dog.java").get
    result.findFile(dog.path).isDefined should be(false)
    f.content.contains("package com.atomist;") should be(true)

    // Should now import Dog
    f.content should include("import com.atomist.Dog;")
  }

  it should "rename class and verify name" in {
    val program =
      """
        |@description "I add FooBar annotations"
        |editor ClassAnnotated
        |
        |with JavaType c
        |do
        |  rename to "Dingo"
      """.stripMargin
    val as = new SimpleFileBasedArtifactSource("", dog)
    val result = executeJava(program, "editors/ClassAnnotated.rug", as)

    val f = result.findFile("src/main/java/com/foo/bar/Dingo.java").get
    result.findFile(dog.path).isDefined should be(false)
    f.content should include("class Dingo")
  }

  it should "verify users of renamed class are updated" is pending

  private def annotateClass(program: String): Unit = {
    val result = executeJava(program, "editors/ClassAnnotated.rug")
    val f = result.findFile("src/main/java/Dog.java").get

    f.content.lines.size should be > 0
    f.content should include("@FooBar")
  }

  it should "add import" in {
    val program =
      """
        |editor ClassAnnotated
        |
        |with JavaSource j
        |with JavaType c
        |do
        |  addImport 'java.util.List'
      """.stripMargin

    val r = executeJava(program, "editors/ClassAnnotated.rug")
    val f = r.findFile("src/main/java/Dog.java").get

    f.content.lines.size should be > 0
    f.content should include("import java.util.List")
  }

  it should "add and remove import" in {
    val program =
      """
        |editor ClassAnnotated
        |
        |with JavaSource j
        |with JavaType c
        |begin
        |  do addImport 'java.util.List'
        |  do removeImport 'java.util.List'
        |end
      """.stripMargin

    val r = executeJava(program, "editors/ClassAnnotated.rug")
    val f = r.findFile("src/main/java/Dog.java").get

    f.content.lines.size should be > 0
    f.content shouldNot include("import java.util.List")
  }

  it should "remove an import" in {
    val program =
      """
        |editor ClassAnnotated
        |
        |with JavaSource j
        |with JavaType c
        |begin
        |  do removeImport 'java.util.Set'
        |end
      """.stripMargin

    val r = executeJava(program, "editors/ClassAnnotated.rug")
    val f = r.findFile("src/main/java/Dog.java").get

    f.content.lines.size should be > 0
    f.content shouldNot include("import java.util.Set")
  }

  it should "not add import for annotation added to class in same package" in pendingUntilFixed {
    val impl = StringFileArtifact("src/main/java/Absquatulator.java",
      """
        |package com.foo;
        |
        |public final class Absquatulator {
        |
        |}
      """.stripMargin)
    val as = new SimpleFileBasedArtifactSource("", Seq(impl))

    val (pkg, ann) = ("com.foo", "Baz")

    val program =
      s"""
        |editor ClassAnnotated
        |
        |with JavaSource j
        |with JavaType c
        |do
        |  addAnnotation '$pkg' '$ann'
      """.stripMargin

    val r = executeJava(program, "editors/ClassAnnotated.rug", as)
    val f = r.findFile(impl.path).get
    f.content.lines.size should be > 0
    f.content shouldNot include(s"import $pkg.$ann")
    f.content should include(s"@$ann")
  }

  it should "act on class that extends given superclass" in {
    val parentFile = StringFileArtifact("src/main/java/NotRelevant.java", "public class NotRelevant {}")
    val childFile = StringFileArtifact("src/main/java/VeryCleverAbsquatulator.java",
      """
        |public class VeryCleverAbsquatulator extends NotRelevant {
        |
        |}
      """.stripMargin)

    val as = new SimpleFileBasedArtifactSource("", Seq(childFile, parentFile))

    val (pkg, ann) = ("com.foo", "Baz")

    val program =
      s"""
        |editor ClassExtended
        |
        |with JavaType when inheritsFrom 'NotRelevant'
        | do
        |   addAnnotation '$pkg' '$ann'
      """.stripMargin

    val r = executeJava(program, "editors/ClassExtended.rug", as)

    val unupdatedParentFile = r.findFile(parentFile.path).get
    unupdatedParentFile shouldEqual parentFile

    val updatedChildFile = r.findFile(childFile.path).get
    updatedChildFile.content should include(s"import $pkg.$ann")
    updatedChildFile.content should include(s"@$ann")
  }

  it should "act on class that extends given abstract superclass" in {
    val parentFile = StringFileArtifact("src/main/java/NotRelevant.java", "public abstract class NotRelevant {}")
    val childFile = StringFileArtifact("src/main/java/VeryCleverAbsquatulator.java",
      """
        |public final class VeryCleverAbsquatulator extends NotRelevant {
        |
        |}
      """.stripMargin)
    val as = new SimpleFileBasedArtifactSource("", Seq(childFile, parentFile))

    val (pkg, ann) = ("com.foo", "Baz")

    val program =
      s"""
         |editor ClassExtended
         |
         |with JavaType when inheritsFrom 'NotRelevant'
         | do
         |   addAnnotation '$pkg' '$ann'
      """.stripMargin

    val r = executeJava(program, "editors/ClassExtended.rug", as)

    val unupdatedParentFile = r.findFile(parentFile.path).get
    unupdatedParentFile shouldEqual parentFile

    val updatedChildFile = r.findFile(childFile.path).get
    updatedChildFile.content should include(s"import $pkg.$ann;")
    updatedChildFile.content should include(s"@$ann")
  }

  it should "add headers to files with long class names" in {
    val notRelevantFile = StringFileArtifact("src/main/java/NotRelevant.java", "public class NotRelevant {}")
    val impl = StringFileArtifact("src/main/java/VeryCleverAbsquatulator.java",
      """
        |public class VeryCleverAbsquatulator implements Absquatulator {
        |
        |}
      """.stripMargin)

    val as = new SimpleFileBasedArtifactSource("", Seq(notRelevantFile, impl))

    val newHeader = "It appears that Rod still likes long names"
    val program =
      s"""
         |editor ClassAnnotated
         |
         |with JavaType c when { c.name().length() > 17 }
         | do
         |   setHeaderComment '$newHeader'
      """.stripMargin

    val r = executeJava(program, "editors/ClassAnnotated.rug", as)

    val unupdatedNotRelevantFile = r.findFile(notRelevantFile.path).get
    unupdatedNotRelevantFile shouldEqual notRelevantFile

    val updatedImpl = r.findFile(impl.path).get
    updatedImpl.content should include(newHeader)

    val newHeader1 = "It appears that Alan would prefer more concise class names"
    val program1 =
      s"""
         |editor ClassAnnotated
         |
         |with JavaType c when { c.name().length() > 17 }
         | do
         |   setHeaderComment '$newHeader1'
      """.stripMargin

    val r1 = executeJava(program1,"editors/ClassAnnotated.rug", as)

    val unupdatedNotRelevantFile1 = r1.findFile(notRelevantFile.path).get
    unupdatedNotRelevantFile1 shouldEqual notRelevantFile

    val updatedImpl1 = r1.findFile(impl.path).get
    updatedImpl1.content should include(newHeader1)
    updatedImpl1.content shouldNot include(newHeader)
  }

  it should "distinguish interfaces from classes" in {
    val interfaceFile = StringFileArtifact("src/main/java/Absquatulator.java", "public interface Absquatulator {}")
    val impl = StringFileArtifact("src/main/java/VeryCleverAbsquatulator.java",
      """
        |public class VeryCleverAbsquatulator implements Absquatulator {
        |
        |}
      """.stripMargin)

    val as = new SimpleFileBasedArtifactSource("", Seq(interfaceFile, impl))

    val program =
      s"""
         |editor ClassAnnotated
         |
         |with JavaType c when isInterface
         | do
         |   addAnnotation 'com.foo.bar' 'Baz'
      """.stripMargin

    val r = executeJava(program,"editors/ClassAnnotated.rug", as)

    val unchangedImpl = r.findFile(impl.path).get
    unchangedImpl.content contentEquals impl.content should be (true)

    val updatedInterface = r.findFile(interfaceFile.path).get
    updatedInterface.content should include(s"import com.foo.bar.Baz;")
    updatedInterface.content should include("@Baz")
  }

  it should "distinguish abstract classes" in {
    val interfaceFile = StringFileArtifact("src/main/java/Absquatulated.java", "public interface Absquatulated {}")
    val abstractFile = StringFileArtifact("src/main/java/AbstractAbsquatulator.java", "public abstract class AbstractAbsquatulator implements Absquatulated {}")
    val concreteFile = StringFileArtifact("src/main/java/Absquatulator.java", "public class Absquatulator extends AbstractAbsquatulator {}")
    val as = new SimpleFileBasedArtifactSource("", Seq(interfaceFile, abstractFile, concreteFile))

    val program =
      s"""
         |editor AbstractClass
         |
         |with JavaType c when isAbstract
         | do
         |   addAnnotation 'com.foo.bar' 'Baz'
      """.stripMargin

    val r = executeJava(program, "editors/AbstractClass.rug", as)

    val updatedInterface = r.findFile(interfaceFile.path).get
    updatedInterface.content should include(s"import com.foo.bar.Baz;")
    updatedInterface.content should include("@Baz")

    val updatedAbstractClass = r.findFile(abstractFile.path).get
    updatedAbstractClass.content should include(s"import com.foo.bar.Baz;")
    updatedAbstractClass.content should include("@Baz")

    val unchangedConcrete = r.findFile(concreteFile.path).get
    unchangedConcrete.content contentEquals concreteFile.content should be (true)
  }

  it should "allow access to project" in {
    val program =
      """
        |@description "I add FooBar annotations"
        |editor ClassAnnotated
        |
        |with JavaType c
        |when {
        |return c.parent().parent().javaFileCount() < 100
        |}
        |do
        |  addAnnotation "com.someone" "FooBar"
      """.stripMargin

    val r = executeJava(program,"editors/ClassAnnotated.rug")
    val f = r.findFile("src/main/java/Dog.java").get

    f.content.lines.size should be > 0
    f.content should include("import com.someone.FooBar;")
    f.content should include("@FooBar")
  }

  it should "annotate constructor" in {
    val program =
      """
        |@description "I add FooBar annotations"
        |editor ClassAnnotated
        |
        |with JavaSource j
        |with JavaType c
        |with constructor ctor when { ctor.parametersSize() == 1 }
        |do
        |  addAnnotation "com.someone" "FooBar"
      """.stripMargin

    val r = executeJava(program,"editors/ClassAnnotated.rug")
    val f = r.findFile("src/main/java/Dog.java").get

    f.content.lines.size should be > 0
    f.content should include("import com.someone.FooBar;")
    f.content should include("@FooBar")
  }

  it should "annotate method" in {
    val program =
      """
        |@description "I add FooBar annotations"
        |editor ClassAnnotated
        |
        |with JavaSource j
        |with JavaType c
        |with method m when { m.name().contains("bark") }
        |do
        |  addAnnotation "com.someone" "FooBar"
      """.stripMargin

    val r = executeJava(program,"editors/ClassAnnotated.rug")
    val f = r.findFile("src/main/java/Dog.java").get

    f.content.lines.size should be > 0
    f.content should include("import com.someone.FooBar;")
    f.content should include("@FooBar")
  }

  it should "remove annotation from method" in {
    val program =
      """
        |@description "I add and remove FooBar annotations"
        |editor ClassAnnotated
        |
        |with JavaSource j
        |with JavaType c
        |with method m when { m.name().contains("bark") }
        |begin
        |  do removeAnnotation "com.someone" "FooBar"
        |end
      """.stripMargin

    val r = executeJava(program,"editors/ClassAnnotated.rug")
    val f = r.findFile("src/main/java/Dog.java").get

    f.content.lines.size should be > 0
    f.content should include("import com.someone.FooBar;")
    f.content shouldNot include("@FooBar")
  }

  it should "annotate field" in {
    val program =
      """
        |@description "I add FooBar annotations"
        |editor ClassAnnotated
        |
        |with JavaSource j
        |with JavaType c
        |with field f when { f.name().contains("Field") && f.parent().name().contains("Dog") }
        |do
        |  addAnnotation "com.someone" "FooBar"
      """.stripMargin

    val r = executeJava(program,"editors/ClassAnnotated.rug")
    val f = r.findFile("src/main/java/Dog.java").get

    f.content.lines.size should be > 0
    f.content should include("import com.someone.FooBar;")
    f.content should include("@FooBar")
  }

  it should "remove annotation from field" in {
    val program =
      """
        |@description "I add and remove FooBar annotations"
        |editor ClassAnnotated
        |
        |with JavaSource j
        |with JavaType c
        |with field f when { f.name().contains("Field") && f.parent().name().contains("Dog") }
        |begin
        |  do removeAnnotation "com.someone" "ComFooBar"
        |end
      """.stripMargin

    val r = executeJava(program,"editors/ClassAnnotated.rug")
    val f = r.findFile("src/main/java/Dog.java").get

    f.content.lines.size should be > 0
    f.content should include("import com.someone.ComFooBar;")
    f.content shouldNot include("@ComFooBar")
  }
}
