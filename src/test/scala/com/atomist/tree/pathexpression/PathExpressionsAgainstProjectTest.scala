package com.atomist.tree.pathexpression

import com.atomist.parse.java.ParsingTargets
import com.atomist.rug.RugCompilerTest
import com.atomist.rug.kind.core.{FileArtifactBackedMutableView, ProjectMutableView}
import com.atomist.rug.kind.java.JavaClassOrInterfaceMutableView
import com.atomist.rug.kind.pom.PomMutableView
import com.atomist.source.{SimpleFileBasedArtifactSource, StringFileArtifact}
import com.atomist.tree.TreeNode
import org.scalatest.{Assertions, FlatSpec, Matchers}

/**
  * Tests for navigating projects using path expressions
  */
class PathExpressionsAgainstProjectTest extends FlatSpec with Matchers with Assertions {

  import com.atomist.tree.pathexpression.PathExpressionParser._

  val ee: ExpressionEngine = new PathExpressionEngine

  it should "#323: error on attempt to find bogus type child" in {
    val proj = ParsingTargets.NonSpringBootMavenProject
    val pmv = new ProjectMutableView(proj)
    val expr = "/DoesntExist()"
    ee.evaluate(pmv, expr) match {
      case Left(err) => assert(err.contains("DoesntExist"))
      case wtf => fail(s"Should have returned an error, not $wtf")
    }
  }

  it should "#323: error on attempt to find bogus type descendant" in {
    val proj = ParsingTargets.NonSpringBootMavenProject
    val pmv = new ProjectMutableView(proj)
    val expr = "//DoesntExist()"
    ee.evaluate(pmv, expr) match {
      case Left(err) => assert(err.contains("DoesntExist"))
      case wtf => fail(s"Should have returned an error, not $wtf")
    }
  }

  it should "#360: handle valid string path attribute on file" in {
    val proj = ParsingTargets.NonSpringBootMavenProject
    val pmv = new ProjectMutableView(proj)
    val expr = "/File()/@path"
    ee.evaluate(pmv, expr) match {
      case Right(nodes) =>
        assert(nodes.size === proj.files.size)
      case wtf => fail(s"Should have returned files, not $wtf")
    }
  }

  it should "#360: handle valid int path attribute on file" in {
    val proj = ParsingTargets.NonSpringBootMavenProject
    val pmv = new ProjectMutableView(proj)
    val expr = "/File()/@lineCount"
    ee.evaluate(pmv, expr) match {
      case Right(nodes) =>
        assert(nodes.size === proj.files.size)
        nodes.foreach(n =>
          assert(n.asInstanceOf[TreeNode].value.toInt > 0)
        )
      case wtf => fail(s"Should have returned files, not $wtf")
    }
  }

  it should "#360: handle valid boolean path attribute on file" in {
    val proj = ParsingTargets.NonSpringBootMavenProject
    val pmv = new ProjectMutableView(proj)
    val expr = "/File()/@isWellFormed"
    ee.evaluate(pmv, expr) match {
      case Right(nodes) =>
        assert(nodes.size === proj.files.size)
        nodes.foreach(n =>
          assert(n.asInstanceOf[TreeNode].value.toBoolean)
        )
      case wtf => fail(s"Should have returned files, not $wtf")
    }
  }

  it should "#360: handle forbidden string path attribute on file" in {
    val proj = ParsingTargets.NonSpringBootMavenProject
    val pmv = new ProjectMutableView(proj)
    val expr = "/File()/@toString"
    ee.evaluate(pmv, expr) match {
      case Left(errMessage) =>
        assert(errMessage.contains("toString"))
      case wtf => fail(s"Should have failed, not returned $wtf")
    }
  }

  // Code exists to support this scenario, but it's hard to think of a type that it could be tested on
  //it should "#360: handle graph node attribute on node"

  it should "not find missing file in project" in {
    val proj = ParsingTargets.NonSpringBootMavenProject
    val pmv = new ProjectMutableView(proj)
    val expr = "/foo"
    val rtn = ee.evaluate(pmv, expr)
    assert(rtn.right.get === Seq())
  }

  it should "find directory contents in project" in {
    val proj = ParsingTargets.NewStartSpringIoProject
    val pmv = new ProjectMutableView(proj)
    val expr = "/src"
    val rtn = ee.evaluate(pmv, expr)
    assert(rtn.right.get.nonEmpty === true)
    val expr2 = "/src/main/java"
    val rtn2 = ee.evaluate(pmv, expr2)
    assert(rtn2.right.get.nonEmpty === true)
  }

  it should "drill into lines inside project" in {
    val proj = ParsingTargets.NewStartSpringIoProject
    val pmv = new ProjectMutableView(proj)
    val expr = "/File()[@name='pom.xml']/Line()"
    val rtn = ee.evaluate(pmv, expr)
    assert(rtn.right.get.nonEmpty === true)
    assert(rtn.right.get.size === proj.findFile("pom.xml").get.content.count(_ == '\n'))
  }

  it should "drill into lines inside project under File" in {
    val proj = ParsingTargets.NewStartSpringIoProject
    val pmv = new ProjectMutableView(proj)
    val expr = "/File()[@name='pom.xml']"
    val rtn = ee.evaluate(pmv, expr)
    assert(rtn.right.get.size === 1)
    val pomFile = rtn.right.get.head.asInstanceOf[FileArtifactBackedMutableView]
    assert(pomFile.nodeTags.contains("File"))
    val expr2 = "//Line()"
    val rtn2 = ee.evaluate(pomFile, expr2)
    assert(rtn2.right.get.nonEmpty === true)
    assert(rtn2.right.get.size > 10)
  }

  it should "find properties file in project" in {
    val proj = ParsingTargets.NewStartSpringIoProject
    val pmv = new ProjectMutableView(proj)
    val expr = "/src//*[@name='application.properties']"
    val rtn = ee.evaluate(pmv, expr)
    assert(rtn.right.get.size === 1)
  }

  it should "find properties file in directory" in {
    val proj = ParsingTargets.NewStartSpringIoProject
    val pmv = new ProjectMutableView(proj)
    val expr = "/src/main/resources/*[@name='application.properties']"
    val rtn = ee.evaluate(pmv, expr)
    assert(rtn.right.get.size === 1)
  }

  it should "jump straight into Java type" in {
    val proj = SimpleFileBasedArtifactSource(
      StringFileArtifact("Test.java", "public class Test {}")
    )
    val pmv = new ProjectMutableView(proj)
    val expr3 = "/JavaType()"
    val rtn3 = ee.evaluate(pmv, expr3)
    // We have left out test classes
    assert(rtn3.right.get.size === 1)
    rtn3.right.get.foreach {
      case j: JavaClassOrInterfaceMutableView =>

    }
  }

  it should "jump into Java type" in {
    val proj = ParsingTargets.NewStartSpringIoProject
    val pmv = new ProjectMutableView(proj)
    val expr = "/src/main/java/com/example/JavaType()"
    val rtn = ee.evaluate(pmv, expr)
    // We have left out test classes
    assert(rtn.right.get.size === 1)
    rtn.right.get.foreach {
      case j: JavaClassOrInterfaceMutableView =>

    }
  }

  it should "find Java type under direct path" in {
    val proj = ParsingTargets.NewStartSpringIoProject
    val pmv = new ProjectMutableView(proj)
    val expr = "/src/main/java/com/example/File()[@name='DemoApplication.java']/JavaType()"
    val rtn = ee.evaluate(pmv, expr)
    assert(rtn.right.get.size === 1)
    rtn.right.get.foreach {
      case j: JavaClassOrInterfaceMutableView =>

    }
  }

  it should "double descend into Java type" in {
    val proj = ParsingTargets.NewStartSpringIoProject
    val pmv = new ProjectMutableView(proj)
    val expr = "/src//JavaType()"
    val rtn = ee.evaluate(pmv, expr)
    assert(rtn.right.get.size === 2)
    rtn.right.get.foreach {
      case j: JavaClassOrInterfaceMutableView =>

    }
  }

  it should "find everything under project" in {
    val pexp = "//*"
    val proj = RugCompilerTest.JavaAndText
    val pmv = new ProjectMutableView(proj)
    val rtn = ee.evaluate(pmv, pexp)
    assert(rtn.right.get.size === 6)
  }

  it should "find every file under project" in {
    val pexp = "//*/File()"
    val proj = RugCompilerTest.JavaAndText
    val pmv = new ProjectMutableView(proj)
    val rtn = ee.evaluate(pmv, pexp)
    assert(rtn.right.get.size === 1)
  }

  it should "find all files under project" in {
    val pexp = "//File()"
    val proj = RugCompilerTest.JavaAndText
    val pmv = new ProjectMutableView(proj)
    val rtn = ee.evaluate(pmv, pexp)
    assert(rtn.right.get.size === 3)
  }

  // This is currently not legal. . can be used later, however
  it should "#361: NOT support navigation into directories starting with ." in pendingUntilFixed(
    fileNavUnder(".atomist")
  )

  it should "support navigation into directories containing special characters" in
    Seq("atomist.home", "123#1", "$atomist", "-atomist", "_atomis_t").foreach(fileNavUnder)

  private def fileNavUnder(directory: String) {
    val pexp = s"/$directory//File()"
    val proj = RugCompilerTest.JavaAndText.withPathAbove(directory)
    val pmv = new ProjectMutableView(proj)
    val rtn = ee.evaluate(pmv, pexp)
    assert(rtn.right.get.size === 3)
  }

  it should "double descend into Java type with superfluous but valid File() type" in {
    val proj = ParsingTargets.NewStartSpringIoProject
    val pmv = new ProjectMutableView(proj)
    val expr = "/src//File()/JavaType()"
    val rtn = ee.evaluate(pmv, expr)
    assert(rtn.right.get.size === 2)
    rtn.right.get.foreach {
      case j: JavaClassOrInterfaceMutableView =>

    }
  }

  it should "double descend into Java type under directory" in {
    val proj = ParsingTargets.NewStartSpringIoProject
    val pmv = new ProjectMutableView(proj)
    val expr = "/src/main/java//File()/JavaType()"
    val rtn = ee.evaluate(pmv, expr)
    assert(rtn.right.get.size === 1)
    rtn.right.get.foreach {
      case j: JavaClassOrInterfaceMutableView =>

    }
  }

  it should "double descend into Java type and select class" in {
    val proj = ParsingTargets.NewStartSpringIoProject
    val pmv = new ProjectMutableView(proj)
    val expr2 = "/src//File()/JavaType()[@name='DemoApplication']"
    val rtn2 = ee.evaluate(pmv, expr2)
    assert(rtn2.right.get.size === 1)
    rtn2.right.get.foreach {
      case j: JavaClassOrInterfaceMutableView =>

    }
  }

  it should "jump into Java type and select class using 2 filters" in {
    val proj = ParsingTargets.NewStartSpringIoProject
    val pmv = new ProjectMutableView(proj)
    val expr2 = "/src/main/java/com/example/JavaType()[@name='DemoApplication' and @type='JavaType']"
    val rtn2 = ee.evaluate(pmv, expr2)
    assert(rtn2.right.get.size === 1)
    rtn2.right.get.foreach {
      case j: JavaClassOrInterfaceMutableView =>
      case x => fail(s"Unexpected: $x")
    }
  }

  it should "jump into Java type and call boolean method" in {
    val proj = ParsingTargets.NewStartSpringIoProject
    val pmv = new ProjectMutableView(proj)
    // Second filter is really a no op
    val expr2 = "/src//JavaType()/*[@type='JavaType' and .isAbstract()]"
    val rtn2 = ee.evaluate(pmv, expr2)
    assert(rtn2.right.get.size === 0)
  }

  it should "handle OR" in {
    val proj = SimpleFileBasedArtifactSource(
      StringFileArtifact("license.txt", "The blah blah license")
    )
    val pmv = new ProjectMutableView(proj)
    // Second filter is really a no op
    val expr = "/*[@name='license.txt' or @name='foo']"
    val rtn = ee.evaluate(pmv, expr)
    assert(rtn.right.get.size === 1)
  }

  it should "jump into Java type and test on name" in {
    val proj = ParsingTargets.NewStartSpringIoProject
    val pmv = new ProjectMutableView(proj)
    // Second filter is really a no op
    val expr = "/src/main/java/com/example/JavaType()[@type='JavaType' and .pkg()='com.example']"
    val rtn = ee.evaluate(pmv, expr)
    assert(rtn.right.get.size === 1)
  }

  it should "match existing types when * is used" in {
    val proj = ParsingTargets.NewStartSpringIoProject
    val pmv = new ProjectMutableView(proj)
    val expr = "/src/main/java/com/example/File()"
    val rtn = ee.evaluate(pmv, expr)
    rtn.right.get.size should be > 0
  }

  it should "match existing types with name" in {
    val proj = SimpleFileBasedArtifactSource(
      StringFileArtifact("src/thing", "content"),
      StringFileArtifact("src/ignore", "content")
    )
    val pmv = new ProjectMutableView(proj)
    val expr = "/src/File()[@name='thing']"
    val rtn = ee.evaluate(pmv, expr)
    assert(rtn.right.get.size === 1)
  }

  it should "test not(predicate) that does veto" in {
    val proj = ParsingTargets.NewStartSpringIoProject
    val pmv = new ProjectMutableView(proj)
    val expr = "/src/main/java/com/example/JavaType()[@type='JavaType' and not(.pkg()='com.example')]"
    val rtn = ee.evaluate(pmv, expr)
    assert(rtn.right.get.isEmpty)
  }

  it should "test not(predicate) that does not veto" in {
    val proj = ParsingTargets.NewStartSpringIoProject
    val pmv = new ProjectMutableView(proj)
    val expr = "/src/main/java/com/example/JavaType()[@type='JavaType' and not(.pkg()='com.wrong')]"
    val rtn = ee.evaluate(pmv, expr)
    assert(rtn.right.get.size === 1)
  }

  it should "allow multiple predicates instead of 'and'" in {
    val proj = ParsingTargets.NewStartSpringIoProject
    val pmv = new ProjectMutableView(proj)
    val expr = "/src/main/java/com/example/JavaType()[.pkg()='com.example']"
    val rtn = ee.evaluate(pmv, expr)
    assert(rtn.right.get.size === 1)
  }

  it should "find files containing Java sources or types" in {
    val types = Set("JavaType", "JavaSource")
    val proj = ParsingTargets.NewStartSpringIoProject
    val pmv = new ProjectMutableView(proj)
    // Second filter is really a no op
    // Second filter is really a no op
    for (nestedType <- types) {
      val expr2 = s"/src//File()[$nestedType()]"
      val rtn2 = ee.evaluate(pmv, expr2)
      val nodes = rtn2.right.get
      nodes.size should be > 1
      nodes.forall(n => n.nodeTags.contains("File")) should be(true)
    }
  }

  it should "not find files containing Elm modules in Java project" in {
    val proj = ParsingTargets.NewStartSpringIoProject
    val pmv = new ProjectMutableView(proj)
    // Second filter is really a no op
    // Second filter is really a no op
    val expr2 = s"/src//File()[/ElmModule()]"
    val rtn2 = ee.evaluate(pmv, expr2)
    val nodes = rtn2.right.get
    assert(nodes.isEmpty === true)
  }

  it should "find all top-level file as descendant" in {
    val proj = ParsingTargets.NewStartSpringIoProject
    val pmv = new ProjectMutableView(proj)
    val expr = "//mvnw"
    val rtn = ee.evaluate(pmv, expr)
    assert(rtn.right.get.size === 1)
    rtn.right.get.foreach {
      case _: FileArtifactBackedMutableView =>

      case x => fail(s"failed to get FileArtifactBackedMutableView: ${x.getClass}")
    }
  }

  it should "find the pom.xml" in {
    val proj = ParsingTargets.MultiPomProject
    val pmv = new ProjectMutableView(proj)
    val expr = "/Pom()"
    val rtn = ee.evaluate(pmv, expr)
    assert(rtn.right.get.size === 1)
    rtn.right.get.foreach {
      case _: PomMutableView =>

      case x => fail(s"failed to get PomMutableView: ${x.getClass}")
    }
  }

  it should "find all the pom.xml descendants" in {
    val proj = ParsingTargets.MultiPomProject
    val pmv = new ProjectMutableView(proj)
    val expr = "//Pom()"
    val rtn = ee.evaluate(pmv, expr)
    assert(rtn.right.get.size === 3)
    rtn.right.get.foreach {
      case _: PomMutableView =>

      case x => fail(s"failed to get PomMutableView: ${x.getClass}")
    }
  }

  it should "find all the pom.xml files" in {
    val proj = ParsingTargets.MultiPomProject
    val pmv = new ProjectMutableView(proj)
    val expr = "/EveryPom()"
    val rtn = ee.evaluate(pmv, expr)
    assert(rtn.right.get.size === 3)
    rtn.right.get.foreach {
      case _: PomMutableView =>

      case x => fail(s"failed to get PomMutableView: ${x.getClass}")
    }
  }

}
