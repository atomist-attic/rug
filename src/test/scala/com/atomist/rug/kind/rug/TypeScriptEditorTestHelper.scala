package com.atomist.rug.kind.rug

import com.atomist.param.SimpleParameterValues
import com.atomist.project.ProjectOperation
import com.atomist.project.archive.SimpleJavaScriptProjectOperationFinder
import com.atomist.project.edit.{ProjectEditor, SuccessfulModification}
import com.atomist.rug.runtime.js.{JavaScriptProjectEditor, JavaScriptProjectOperationFinder}
import com.atomist.rug.ts.{RugTranspiler, TypeScriptBuilder}
import com.atomist.rug.{CompilerChainPipeline, RugPipeline}
import com.atomist.source.{ArtifactSource, SimpleFileBasedArtifactSource, StringFileArtifact}
import org.scalatest.Matchers

trait TypeScriptEditorTestHelper extends Matchers {

  val typeScriptPipeline: RugPipeline =
    new CompilerChainPipeline(Seq(new RugTranspiler(), TypeScriptBuilder.compiler))

  def rugArchiveFromEditor(editorName: String, program: String) = {
    val as = SimpleFileBasedArtifactSource(new StringFileArtifact(name = editorName + ".ts", path = ".atomist/editors/" + editorName + ".ts", content = program))
    TypeScriptBuilder.compileWithModel(as)
  }

  def executeTypescript(rugArchive: ArtifactSource,
                        target: ArtifactSource,
                        params: Map[String, String] = Map(),
                        others: Seq[ProjectOperation] = Nil): ArtifactSource = {

    val eds = SimpleJavaScriptProjectOperationFinder.find(rugArchive).editors

    if (eds.isEmpty) {
      throw new Exception("No editor was parsed")
    }

    val jsed = eds.head.asInstanceOf[JavaScriptProjectEditor]
    jsed.setContext(others)

    val pe = eds.head
    pe.modify(target, SimpleParameterValues(params)) match {
      case sm: SuccessfulModification =>
        sm.result
      case um =>
        fail("This modification was not successful: " + um)
    }
  }
}
