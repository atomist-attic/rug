package com.atomist.rug.kind.rug

import com.atomist.project.SimpleProjectOperationArguments
import com.atomist.project.edit.{ProjectEditor, SuccessfulModification}
import com.atomist.rug.DefaultRugPipeline
import com.atomist.rug.InterpreterRugPipeline._
import com.atomist.rug.kind.DefaultTypeRegistry
import com.atomist.rug.parser.{ParserCombinatorRugParser, RugParser}
import com.atomist.source.{ArtifactSource, SimpleFileBasedArtifactSource, StringFileArtifact}
import org.scalatest.Matchers

trait RugEditorTestHelper extends Matchers {

  def executeRug(program: String, startingProject: ArtifactSource,
                 params: Map[String, String] = Map()): ArtifactSource = {
    val runtime = new DefaultRugPipeline(DefaultTypeRegistry)


    val editorName = parseRugForEditorName(program)
    val editorPath = s".atomist/editors/$editorName.rug"

    val rugArchive = new SimpleFileBasedArtifactSource(DefaultRugArchive,
      StringFileArtifact(editorPath, program))


    val eds = runtime.create(rugArchive,None)
    assert(eds.size === 1)
    val pe = eds.head.asInstanceOf[ProjectEditor]

    val r = pe.modify(startingProject, SimpleProjectOperationArguments("", params))
    r match {
      case sm: SuccessfulModification =>
        sm.result
      case um =>
        fail("This modification was not successful: " + um)
    }
  }

  private  def parseRugForEditorName(program: String) =
    new ParserCombinatorRugParser().parse(program).head.name

}
