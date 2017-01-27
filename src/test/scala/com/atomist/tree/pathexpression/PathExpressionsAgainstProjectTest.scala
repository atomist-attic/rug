package com.atomist.tree.pathexpression

import com.atomist.parse.java.ParsingTargets
import com.atomist.project.archive.DefaultAtomistConfig
import com.atomist.rug.RugCompilerTest
import com.atomist.rug.kind.DefaultTypeRegistry
import com.atomist.rug.kind.core.ProjectMutableView
import com.atomist.rug.kind.elm.ElmModuleMutableView
import com.atomist.rug.kind.java.JavaClassOrInterfaceView
import com.atomist.source.{EmptyArtifactSource, SimpleFileBasedArtifactSource, StringFileArtifact}
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
    val expr = "/foo"
    val rtn = ee.evaluate(pmv, expr, DefaultTypeRegistry)
    rtn.right.get should equal(Seq())
  }

  it should "find directory contents in project" in {
    val proj = ParsingTargets.NewStartSpringIoProject
    val pmv = new ProjectMutableView(EmptyArtifactSource(""), proj, DefaultAtomistConfig)
    val expr = "/src"
    val rtn = ee.evaluate(pmv, expr, DefaultTypeRegistry)
    rtn.right.get.nonEmpty should be(true)
    val expr2 = "/src/main/java"
    val rtn2 = ee.evaluate(pmv, expr2, DefaultTypeRegistry)
    rtn2.right.get.nonEmpty should be(true)
  }

  it should "find properties file in project" in {
    val proj = ParsingTargets.NewStartSpringIoProject
    val pmv = new ProjectMutableView(EmptyArtifactSource(""), proj, DefaultAtomistConfig)
    val expr = "/src//*[@name='application.properties']"
    val rtn = ee.evaluate(pmv, expr, DefaultTypeRegistry)
    rtn.right.get.size should be(1)
  }

  it should "find properties file in directory" in {
    val proj = ParsingTargets.NewStartSpringIoProject
    val pmv = new ProjectMutableView(EmptyArtifactSource(""), proj, DefaultAtomistConfig)
    val expr = "/src/main/resources/*[@name='application.properties']"
    val rtn = ee.evaluate(pmv, expr, DefaultTypeRegistry)
    rtn.right.get.size should be(1)
  }

  it should "jump straight into Java type" in {
    val proj = SimpleFileBasedArtifactSource(
      StringFileArtifact("Test.java", "public class Test {}")
    )
    val pmv = new ProjectMutableView(EmptyArtifactSource(""), proj, DefaultAtomistConfig)
    val expr3 = "/JavaType()"
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
    val expr3 = "/SpringBootProject()"
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
    val expr = "/src/main/java/com/example/JavaType()"
    val rtn = ee.evaluate(pmv, expr, DefaultTypeRegistry)
    // We have left out test classes
    rtn.right.get.size should be(1)
    rtn.right.get.foreach {
      case j: JavaClassOrInterfaceView =>
    }
  }

  it should "find Java type under direct path" in {
    val proj = ParsingTargets.NewStartSpringIoProject
    val pmv = new ProjectMutableView(EmptyArtifactSource(""), proj, DefaultAtomistConfig)
    val expr = "/src/main/java/com/example/File()[@name='DemoApplication.java']/JavaType()"
    val rtn = ee.evaluate(pmv, expr, DefaultTypeRegistry)
    rtn.right.get.size should be(1)
    rtn.right.get.foreach {
      case j: JavaClassOrInterfaceView =>
    }
  }

  it should "double descend into Java type" in {
    val proj = ParsingTargets.NewStartSpringIoProject
    val pmv = new ProjectMutableView(EmptyArtifactSource(""), proj, DefaultAtomistConfig)
    val expr = "/src//JavaType()"
    val rtn = ee.evaluate(pmv, expr, DefaultTypeRegistry)
    rtn.right.get.size should be(2)
    rtn.right.get.foreach {
      case j: JavaClassOrInterfaceView =>
    }
  }

  it should "find everything under project" in {
    val pexp = "//*"
    val proj = RugCompilerTest.JavaAndText
    val pmv = new ProjectMutableView(EmptyArtifactSource(""), proj, DefaultAtomistConfig)
    val rtn = ee.evaluate(pmv, pexp, DefaultTypeRegistry)
    rtn.right.get.size should be(6)
  }

  it should "find every file under project" in {
    val pexp = "//*/File()"
    val proj = RugCompilerTest.JavaAndText
    val pmv = new ProjectMutableView(EmptyArtifactSource(""), proj, DefaultAtomistConfig)
    val rtn = ee.evaluate(pmv, pexp, DefaultTypeRegistry)
    rtn.right.get.size should be(1)
  }

  it should "find all files under project" in {
    val pexp = "//File()"
    val proj = RugCompilerTest.JavaAndText
    val pmv = new ProjectMutableView(EmptyArtifactSource(""), proj, DefaultAtomistConfig)
    val rtn = ee.evaluate(pmv, pexp, DefaultTypeRegistry)
    rtn.right.get.size should be(3)
    //rtn.right.get.map(_.)
  }

  it should "double descend into Java type with superfluous but valid File() type" in {
    val proj = ParsingTargets.NewStartSpringIoProject
    val pmv = new ProjectMutableView(EmptyArtifactSource(""), proj, DefaultAtomistConfig)
    val expr = "/src//File()/JavaType()"
    val rtn = ee.evaluate(pmv, expr, DefaultTypeRegistry)
    rtn.right.get.size should be(2)
    rtn.right.get.foreach {
      case j: JavaClassOrInterfaceView =>
    }
  }

  it should "double descend into Java type under directory" in {
    val proj = ParsingTargets.NewStartSpringIoProject
    val pmv = new ProjectMutableView(EmptyArtifactSource(""), proj, DefaultAtomistConfig)
    val expr = "/src/main/java//File()/JavaType()"
    val rtn = ee.evaluate(pmv, expr, DefaultTypeRegistry)
    rtn.right.get.size should be(1)
    rtn.right.get.foreach {
      case j: JavaClassOrInterfaceView =>
    }
  }

  it should "double descend into Java type and select class" in {
    val proj = ParsingTargets.NewStartSpringIoProject
    val pmv = new ProjectMutableView(EmptyArtifactSource(""), proj, DefaultAtomistConfig)
    val expr2 = "/src//File()/JavaType()[@name='DemoApplication']"
    val rtn2 = ee.evaluate(pmv, expr2, DefaultTypeRegistry)
    rtn2.right.get.size should be(1)
    rtn2.right.get.foreach {
      case j: JavaClassOrInterfaceView =>
    }
  }

  it should "jump into Java type and select class using 2 filters" in {
    val proj = ParsingTargets.NewStartSpringIoProject
    val pmv = new ProjectMutableView(EmptyArtifactSource(""), proj, DefaultAtomistConfig)
    val expr2 = "/src/main/java/com/example/JavaType()[@name='DemoApplication' and @type='JavaType']"
    val rtn2 = ee.evaluate(pmv, expr2, DefaultTypeRegistry)
    rtn2.right.get.size should be(1)
    rtn2.right.get.foreach {
      case j: JavaClassOrInterfaceView =>
    }
  }

  it should "jump into Java type and call boolean method" in {
    val proj = ParsingTargets.NewStartSpringIoProject
    val pmv = new ProjectMutableView(EmptyArtifactSource(""), proj, DefaultAtomistConfig)
    // Second filter is really a no op
    val expr2 = "/src//JavaType()/*[@type='JavaType' and .isAbstract()]"
    val rtn2 = ee.evaluate(pmv, expr2, DefaultTypeRegistry)
    rtn2.right.get.size should be(0)
  }

  it should "handle OR" in {
    val proj = SimpleFileBasedArtifactSource(
      StringFileArtifact("license.txt", "The blah blah license")
    )
    val pmv = new ProjectMutableView(EmptyArtifactSource(""), proj, DefaultAtomistConfig)
    // Second filter is really a no op
    val expr = "/*[@name='license.txt' or @name='foo']"
    val rtn = ee.evaluate(pmv, expr, DefaultTypeRegistry)
    rtn.right.get.size should be(1)
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
    val expr = "/src/main/java/com/example/JavaType()[@type='JavaType' and .pkg()='com.example']"
    val rtn = ee.evaluate(pmv, expr, DefaultTypeRegistry)
    rtn.right.get.size should be(1)
  }

  // We know longer support this behavior, of refusing to do a type jump
  //  it should "not jump when * is used" in {
  //    val proj = ParsingTargets.NewStartSpringIoProject
  //    val pmv = new ProjectMutableView(EmptyArtifactSource(""), proj, DefaultAtomistConfig)
  //    // Second filter is really a no op
  //    val expr = "/src/main/java/com/example/*:java.class[type='java.class' and .pkg()='com.example']"
  //    val rtn = ee.evaluate(pmv, expr, DefaultTypeRegistry)
  //    rtn.right.get.size should be (0)
  //  }

  it should "match existing types when * is used" in {
    val proj = ParsingTargets.NewStartSpringIoProject
    val pmv = new ProjectMutableView(EmptyArtifactSource(""), proj, DefaultAtomistConfig)
    val expr = "/src/main/java/com/example/File()"
    val rtn = ee.evaluate(pmv, expr, DefaultTypeRegistry)
    rtn.right.get.size should be > 0
  }

  it should "match existing types with name" in {
    val proj = SimpleFileBasedArtifactSource(
      StringFileArtifact("src/thing", "content"),
      StringFileArtifact("src/ignore", "content")
    )
    val pmv = new ProjectMutableView(EmptyArtifactSource(""), proj, DefaultAtomistConfig)
    val expr = "/src/File()[@name='thing']"
    val rtn = ee.evaluate(pmv, expr, DefaultTypeRegistry)
    rtn.right.get.size should be(1)
  }

  it should "test not(predicate)" in {
    val proj = ParsingTargets.NewStartSpringIoProject
    val pmv = new ProjectMutableView(EmptyArtifactSource(""), proj, DefaultAtomistConfig)
    val expr = "/src/main/java/com/example/JavaType()[@type='JavaType' and not(.pkg()='com.wrong')]"
    val rtn = ee.evaluate(pmv, expr, DefaultTypeRegistry)
    rtn.right.get.size should be(1)
  }

  it should "allow multiple predicates instead of 'and'" in {
    val proj = ParsingTargets.NewStartSpringIoProject
    val pmv = new ProjectMutableView(EmptyArtifactSource(""), proj, DefaultAtomistConfig)
    val expr = "/src/main/java/com/example/JavaType()[.pkg()='com.example']"
    val rtn = ee.evaluate(pmv, expr, DefaultTypeRegistry)
    rtn.right.get.size should be(1)
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
    val expr2 = "/src/ElmModule()[.exposes('main')]"
    val rtn2 = ee.evaluate(pmv, expr2, DefaultTypeRegistry)
    rtn2.right.get.size should be(1)
    rtn2.right.get.head match {
      case em: ElmModuleMutableView =>
        em.name should equal("Main")
    }
  }

  it should "find files containing Java sources or types" in {
    val types = Set("JavaType", "JavaSource")
    val proj = ParsingTargets.NewStartSpringIoProject
    val pmv = new ProjectMutableView(EmptyArtifactSource(""), proj, DefaultAtomistConfig)
    // Second filter is really a no op
    for (nestedType <- types) {
      val expr2 = s"/src//File()[$nestedType()]"
      val rtn2 = ee.evaluate(pmv, expr2, DefaultTypeRegistry)
      val nodes = rtn2.right.get
      nodes.size should be > 1
      nodes.forall(n => n.nodeType.contains("File")) should be(true)
    }
  }

  it should "not find files containing Elm modules in Java project" in {
    val proj = ParsingTargets.NewStartSpringIoProject
    val pmv = new ProjectMutableView(EmptyArtifactSource(""), proj, DefaultAtomistConfig)
    // Second filter is really a no op
    val expr2 = s"/src//File()[/ElmModule()]"
    val rtn2 = ee.evaluate(pmv, expr2, DefaultTypeRegistry)
    val nodes = rtn2.right.get
    nodes.isEmpty should be (true)
  }

}
