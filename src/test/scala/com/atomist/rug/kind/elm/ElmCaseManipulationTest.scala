package com.atomist.rug.kind.elm

import com.atomist.project.SimpleProjectOperationArguments
import com.atomist.project.edit.{ProjectEditor, SuccessfulModification}
import com.atomist.rug.DefaultRugPipeline
import com.atomist.rug.InterpreterRugPipeline.DefaultRugArchive
import com.atomist.rug.kind.DefaultTypeRegistry
import com.atomist.source.{ArtifactSource, SimpleFileBasedArtifactSource, StringFileArtifact}
import org.scalatest.{FlatSpec, Matchers}
import org.scalatest.OptionValues._

class ElmCaseManipulationTest extends FlatSpec with Matchers {

  private val bodyAppenderUnderUpdateFunction: String =
    """
      |editor AddClause
      |
      |with ElmModule when name = 'Main'
      |  with function f when name = 'update'
      |    with case cc
      |      begin
      |         do replaceBody { cc.body() + " ! []" }
      |      end
      |
    """.stripMargin

  private val bodyAppenderMatchingOnCaseExpression: String =
    """
      |editor AddClause
      |
      |with ElmModule when name = 'Main'
      |    with case cc when matchAsString = 'msg'
      |      begin
      |         do replaceBody { cc.body() + " ! []" }
      |      end
      |
    """.stripMargin

  private val clauseAdderMatchingExpression: String =
    """
      |editor AddClause
      |
      |param expr: ^.*$
      |param rhs: ^.*$
      |
      |with ElmModule when name = 'Main'
      |    with case cc when matchAsString = 'msg'
      |      begin
      |         do addClause expr rhs
      |      end
      |
    """.stripMargin

  it should "append to case statement under update function" in
    appendToCase(bodyAppenderUnderUpdateFunction)

  it should "append to case statement matching case expression" in
    appendToCase(bodyAppenderMatchingOnCaseExpression)

  it should "add clause to case statement matching case expression" in
    addClauseToCase(clauseAdderMatchingExpression)

  private def appendToCase(rugProg: String) {
    val todoSource = StringFileArtifact("Main.elm", ElmParserTest.FullProgram)
    val r = elmExecute(new SimpleFileBasedArtifactSource("", todoSource), rugProg)
    val f = r.findFile("Main.elm").value
    val content = f.content
    content should include("! []")
  }

  private def addClauseToCase(rugProg: String) {
    val todoSource = StringFileArtifact("Main.elm", ElmParserTest.FullProgram)
    val expr = "Expression"
    val rhs = "bar"
    val r = elmExecute(new SimpleFileBasedArtifactSource("", todoSource), rugProg, Map(
      "expr" -> expr,
      "rhs" -> rhs
    ))
    val f = r.findFile("Main.elm").value
    val content = f.content
    content should (
      include(expr)
        and
        include(rhs)
      )
  }

  private def elmExecute(elmProject: ArtifactSource, program: String,
                         params: Map[String, String] = Map()): ArtifactSource = {
    val runtime = new DefaultRugPipeline(DefaultTypeRegistry)

    val as = new SimpleFileBasedArtifactSource(DefaultRugArchive, StringFileArtifact(runtime.defaultFilenameFor(program), program))
    val eds = runtime.create(as,None)
    eds.size should be(1)
    val pe = eds.head.asInstanceOf[ProjectEditor]

    val r = pe.modify(elmProject, SimpleProjectOperationArguments("", params))
    r match {
      case sm: SuccessfulModification =>
        //show(sm.result)
        sm.result
      case _ => ???
    }
  }

}
