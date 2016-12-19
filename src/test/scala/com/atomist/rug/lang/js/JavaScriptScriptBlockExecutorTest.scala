package com.atomist.rug.lang.js

import com.atomist.util.scalaparsing.JavaScriptBlock
import com.atomist.rug.kind.core.ProjectMutableView
import com.atomist.rug.kind.java.JavaTypeUsageTest._
import com.atomist.rug.kind.java.{JavaTypeUsageTest, JavaProjectMutableView, SpringBootProjectMutableView, SpringProjectMutableView}
import com.atomist.rug.parser.ScriptBlockAction
import com.atomist.rug.runtime.lang.{DefaultScriptBlockActionExecutor, ScriptBlockActionExecutor}
import com.atomist.source.EmptyArtifactSource
import org.scalatest.{FlatSpec, Matchers}

class JavaScriptScriptBlockExecutorTest extends FlatSpec with Matchers {

  it should "traverse structure of the project" in {
    val project = new ProjectMutableView(EmptyArtifactSource(""), JavaTypeUsageTest.JavaAndText)

    val script =
      """
        |var print = function(s) {}
        |for (i = 0; i < project.files().length; i++) {
        |    print("The file is" + project.files().get(i));
        |}
      """.stripMargin

    DefaultScriptBlockActionExecutor.execute(ScriptBlockAction(JavaScriptBlock(script)), project,
      ScriptBlockActionExecutor.DEFAULT_PROJECT_ALIAS, Map())
  }

  it should "check that a file exists from javscript" in {
    val project = new ProjectMutableView(EmptyArtifactSource(""), JavaTypeUsageTest.JavaAndText)

    val script = """return project.fileExists("pom.xml");""".stripMargin

    val result = DefaultScriptBlockActionExecutor.execute(ScriptBlockAction(JavaScriptBlock(script)), project,
      ScriptBlockActionExecutor.DEFAULT_PROJECT_ALIAS, Map())

    result.asInstanceOf[Boolean] should be(true)
  }

  it should "respond if the project is a Maven project, tested from javscript" in {
    val project = new SpringBootProjectMutableView(new SpringProjectMutableView(
      new JavaProjectMutableView(new ProjectMutableView(EmptyArtifactSource(""), NewSpringBootProject))))

    val script = """return project.isMaven() """.stripMargin

    val result = DefaultScriptBlockActionExecutor.execute(ScriptBlockAction(JavaScriptBlock(script)), project,
      ScriptBlockActionExecutor.DEFAULT_PROJECT_ALIAS, Map())

    result.asInstanceOf[Boolean] should be(true)
  }

  it should "allow the javascript to work with parameters" in {
    val project = new SpringBootProjectMutableView(new SpringProjectMutableView(
      new JavaProjectMutableView(new ProjectMutableView(EmptyArtifactSource(""), NewSpringBootProject))))

    val identifierKey = "text"
    val identifierValue = "Hi there"

    val script = s"""return $identifierKey;""".stripMargin

    val result = DefaultScriptBlockActionExecutor.execute(ScriptBlockAction(JavaScriptBlock(script)), project,
      ScriptBlockActionExecutor.DEFAULT_PROJECT_ALIAS,
      Map(
        identifierKey -> identifierValue))

    result.asInstanceOf[String] should be(identifierValue)
  }

  it should "allow the javacript to return a string with curly brackets" in {
    val project = new ProjectMutableView(EmptyArtifactSource(""), JavaTypeUsageTest.JavaAndText)

    val script =
      """ var print = function(s) {}
          print("{}")
      """.stripMargin

    DefaultScriptBlockActionExecutor.execute(ScriptBlockAction(JavaScriptBlock(script)), project,
      ScriptBlockActionExecutor.DEFAULT_PROJECT_ALIAS, Map())
  }
}
