package com.atomist.tree.content.text

import com.atomist.parse.java.ParsingTargets
import com.atomist.project.archive.DefaultAtomistConfig
import com.atomist.rug.kind.DefaultTypeRegistry
import com.atomist.rug.kind.core.ProjectMutableView
import com.atomist.rug.kind.elm.ElmModuleMutableView
import com.atomist.rug.kind.java.JavaClassOrInterfaceView
import com.atomist.source.{EmptyArtifactSource, SimpleFileBasedArtifactSource, StringFileArtifact}
import com.atomist.tree.pathexpression.{ExpressionEngine, PathExpressionEngine}
import org.scalatest.{FlatSpec, Matchers}

/**
  * Tests for navigating project
  */
class PathExpressionsAgainstProjectTest extends FlatSpec with Matchers {

  import com.atomist.tree.pathexpression.PathExpressionParser._

  val ee: ExpressionEngine = new PathExpressionEngine

  it should "not find missing property in project" in {
    val proj = ParsingTargets.NonSpringBootMavenProject
    val pmv = new ProjectMutableView(EmptyArtifactSource(""), proj, DefaultAtomistConfig)
    val expr = "foo"
    val rtn = ee.evaluate(pmv, expr, DefaultTypeRegistry)
    rtn.right.get should equal(Seq())
  }

  it should "find directory contents in project" in {
    val proj = ParsingTargets.NewStartSpringIoProject
    val pmv = new ProjectMutableView(EmptyArtifactSource(""), proj, DefaultAtomistConfig)
    val expr = "src"
    val rtn = ee.evaluate(pmv, expr, DefaultTypeRegistry)
    rtn.right.get.nonEmpty should be (true)
    val expr2 = "src/main/java"
    val rtn2 = ee.evaluate(pmv, expr2, DefaultTypeRegistry)
    rtn2.right.get.nonEmpty should be (true)
  }

  it should "find properties file in project" in {
    val proj = ParsingTargets.NewStartSpringIoProject
    val pmv = new ProjectMutableView(EmptyArtifactSource(""), proj, DefaultAtomistConfig)
    val expr = "src//*[name='application.properties']"
    val rtn = ee.evaluate(pmv, expr, DefaultTypeRegistry)
    rtn.right.get.size should be (1)
  }

  it should "find properties file in directory" in {
    val proj = ParsingTargets.NewStartSpringIoProject
    val pmv = new ProjectMutableView(EmptyArtifactSource(""), proj, DefaultAtomistConfig)
    val expr = "src/main/resources/*[name='application.properties']"
    val rtn = ee.evaluate(pmv, expr, DefaultTypeRegistry)
    rtn.right.get.size should be (1)
  }

  it should "jump straight into Java type" in {
    val proj = SimpleFileBasedArtifactSource(
      StringFileArtifact("Test.java", "public class Test {}")
    )
    val pmv = new ProjectMutableView(EmptyArtifactSource(""), proj, DefaultAtomistConfig)
    val expr3 = "->java.class"
    val rtn3 = ee.evaluate(pmv, expr3, DefaultTypeRegistry)
    // We have left out test classes
    rtn3.right.get.size should be(1)
    rtn3.right.get.foreach {
      case j: JavaClassOrInterfaceView =>
    }
  }

  it should "jump straight into Spring Boot type" in {
    val proj = SimpleFileBasedArtifactSource(
      StringFileArtifact("Test.java", "public class Test {}")
    )
    val pmv = new ProjectMutableView(EmptyArtifactSource(""), proj, DefaultAtomistConfig)
    val expr3 = "->spring.bootProject"
    val rtn3 = ee.evaluate(pmv, expr3, DefaultTypeRegistry)
    // We have left out test classes
    rtn3.right.get.size should be(0)
//    rtn3.right.get.foreach {
//      case j: JavaClassOrInterfaceView =>
//    }
  }

  it should "jump into Java type" in {
    val proj = ParsingTargets.NewStartSpringIoProject
    val pmv = new ProjectMutableView(EmptyArtifactSource(""), proj, DefaultAtomistConfig)

    val expr3 = "src/main/java/com/example/->java.class"
    val rtn3 = ee.evaluate(pmv, expr3, DefaultTypeRegistry)
    // We have left out test classes
    rtn3.right.get.size should be(1)
    rtn3.right.get.foreach {
      case j: JavaClassOrInterfaceView =>
    }
  }

  it should "accept starting with ." in {
    val proj = ParsingTargets.NewStartSpringIoProject
    val pmv = new ProjectMutableView(EmptyArtifactSource(""), proj, DefaultAtomistConfig)
    val expr3 = ".*[true]/src/main/java/com/example/->java.class"
    val rtn3 = ee.evaluate(pmv, expr3, DefaultTypeRegistry)
    // We have left out test classes
    rtn3.right.get.size should be(1)
    rtn3.right.get.foreach {
      case j: JavaClassOrInterfaceView =>
    }
  }

  it should "double descend into Java type" in {
    val proj = ParsingTargets.NewStartSpringIoProject
    val pmv = new ProjectMutableView(EmptyArtifactSource(""), proj, DefaultAtomistConfig)
    val expr2 = "src//*:file->java.class"
    val rtn2 = ee.evaluate(pmv, expr2, DefaultTypeRegistry)
    rtn2.right.get.size should be(2)
    rtn2.right.get.foreach {
      case j: JavaClassOrInterfaceView =>
    }
  }

  it should "double descend into Java type under directory" in {
    val proj = ParsingTargets.NewStartSpringIoProject
    val pmv = new ProjectMutableView(EmptyArtifactSource(""), proj, DefaultAtomistConfig)
    val expr2 = "src/main/java//*:file->java.class"
    val rtn2 = ee.evaluate(pmv, expr2, DefaultTypeRegistry)
    rtn2.right.get.size should be(1)
    rtn2.right.get.foreach {
      case j: JavaClassOrInterfaceView =>
    }
  }

  it should "double descend into Java type and select class" in {
    val proj = ParsingTargets.NewStartSpringIoProject
    val pmv = new ProjectMutableView(EmptyArtifactSource(""), proj, DefaultAtomistConfig)
    val expr2 = "src//*:file->java.class[name='DemoApplication']"
    val rtn2 = ee.evaluate(pmv, expr2, DefaultTypeRegistry)
    rtn2.right.get.size should be (1)
    rtn2.right.get.foreach {
      case j: JavaClassOrInterfaceView =>
    }
  }

  it should "jump into Java type and select class using 2 filters" in {
    val proj = ParsingTargets.NewStartSpringIoProject
    val pmv = new ProjectMutableView(EmptyArtifactSource(""), proj, DefaultAtomistConfig)
    val expr2 = "src/main/java/com/example/->java.class[name='DemoApplication' and type='java.class']"
    val rtn2 = ee.evaluate(pmv, expr2, DefaultTypeRegistry)
    rtn2.right.get.size should be (1)
    rtn2.right.get.foreach {
      case j: JavaClassOrInterfaceView =>
    }
  }

  it should "jump into Java type and call boolean method" in {
    val proj = ParsingTargets.NewStartSpringIoProject
    val pmv = new ProjectMutableView(EmptyArtifactSource(""), proj, DefaultAtomistConfig)
    // Second filter is really a no op
    val expr2 = "src//->java.class/[type='java.class' and .isAbstract()]"
    val rtn2 = ee.evaluate(pmv, expr2, DefaultTypeRegistry)
    rtn2.right.get.size should be (0)
  }

  it should "handle OR" in {
    val proj = SimpleFileBasedArtifactSource(
      StringFileArtifact("license.txt", "The blah blah license")
    )
    val pmv = new ProjectMutableView(EmptyArtifactSource(""), proj, DefaultAtomistConfig)
    // Second filter is really a no op
    val expr = "/[name='license.txt' or name='foo']"
    val rtn = ee.evaluate(pmv, expr, DefaultTypeRegistry)
    rtn.right.get.size should be (1)
  }

//  it should "find files irrespective of case" in {
//    val proj = SimpleFileBasedArtifactSource(
//      StringFileArtifact("license.txt", "The blah blah license")
//    )
//    val pmv = new ProjectMutableView(EmptyArtifactSource(""), proj, DefaultAtomistConfig)
//
//    // Second filter is really a no op
//    val expr = "/[name='license.txt' or name='foo']"
//    val rtn = ee.evaluate(pmv, expr)
//    rtn.right.get.size should be (1)
//  }

  it should "jump into Java type and test on name" in {
    val proj = ParsingTargets.NewStartSpringIoProject
    val pmv = new ProjectMutableView(EmptyArtifactSource(""), proj, DefaultAtomistConfig)
    // Second filter is really a no op
    val expr = "src/main/java/com/example/->java.class[type='java.class' and .pkg()='com.example']"
    val rtn = ee.evaluate(pmv, expr, DefaultTypeRegistry)
    rtn.right.get.size should be (1)
  }

  it should "not jump when * is used" in {
    val proj = ParsingTargets.NewStartSpringIoProject
    val pmv = new ProjectMutableView(EmptyArtifactSource(""), proj, DefaultAtomistConfig)
    // Second filter is really a no op
    val expr = "src/main/java/com/example/*:java.class[type='java.class' and .pkg()='com.example']"
    val rtn = ee.evaluate(pmv, expr, DefaultTypeRegistry)
    rtn.right.get.size should be (0)
  }

  it should "match existing types when * is used" in {
    val proj = ParsingTargets.NewStartSpringIoProject
    val pmv = new ProjectMutableView(EmptyArtifactSource(""), proj, DefaultAtomistConfig)

    // Second filter is really a no op
    val expr = "src/main/java/com/example/*:file"
    val rtn = ee.evaluate(pmv, expr, DefaultTypeRegistry)
    rtn.right.get.size should be >(0)
  }

  it should "match existing types with name" in {
    val proj = SimpleFileBasedArtifactSource(
      StringFileArtifact("src/thing", "content"),
      StringFileArtifact("src/ignore", "content")
    )
    val pmv = new ProjectMutableView(EmptyArtifactSource(""), proj, DefaultAtomistConfig)
    val expr = "src/thing:file"
    val rtn = ee.evaluate(pmv, expr, DefaultTypeRegistry)
    rtn.right.get.size should be (1)
  }

  it should "test not(predicate)" in {
    val proj = ParsingTargets.NewStartSpringIoProject
    val pmv = new ProjectMutableView(EmptyArtifactSource(""), proj, DefaultAtomistConfig)
    val expr = "src/main/java/com/example/->java.class[type='java.class' and not(.pkg()='com.wrong')]"
    val rtn = ee.evaluate(pmv, expr, DefaultTypeRegistry)
    rtn.right.get.size should be (1)
  }

  it should "allow multiple predicates instead of 'and'" in {
    val proj = ParsingTargets.NewStartSpringIoProject
    val pmv = new ProjectMutableView(EmptyArtifactSource(""), proj, DefaultAtomistConfig)
    val expr = "src/main/java/com/example/->java.class[type='java.class'][.pkg()='com.example']"
    val rtn = ee.evaluate(pmv, expr, DefaultTypeRegistry)
    rtn.right.get.size should be (1)
  }

  it should "select Elm class using method" in {
    val elmWithMain =
      """
        |module Main exposing (main)
        |
        |import Html
        |import Html.Attributes
        |
        |main : Html.Html
        |main =
        |  Html.div
        |    [ Html.Attributes.class "wrapper" ]
        |    [ Html.h1
        |      [ Html.Attributes.class "headline" ]
        |      [ Html.text "Hello World" ]
        |    , Html.p []
        |      [ Html.text "HTML, with qualified imports." ]
        |    ]
      """.stripMargin
    val proj = SimpleFileBasedArtifactSource(StringFileArtifact("src/Main.elm", elmWithMain))
    val pmv = new ProjectMutableView(EmptyArtifactSource(""), proj, DefaultAtomistConfig)
    val expr2 = "src/->ElmModule[.exposes('main')]"
    val rtn2 = ee.evaluate(pmv, expr2, DefaultTypeRegistry)
    rtn2.right.get.size should be (1)
    rtn2.right.get.head match {
      case em: ElmModuleMutableView =>
        em.name should equal("Main")
    }
  }

}