package com.atomist.rug.kind.rug

import com.atomist.param.SimpleParameterValues
import com.atomist.project.ProjectOperation
import com.atomist.project.archive.SimpleJavaScriptProjectOperationFinder
import com.atomist.project.edit.SuccessfulModification
import com.atomist.rug.runtime.js.JavaScriptProjectEditor
import com.atomist.rug.ts.TypeScriptBuilder
import com.atomist.source.{ArtifactSource, SimpleFileBasedArtifactSource, StringFileArtifact}
import org.scalatest.Matchers

trait TypeScriptEditorTestHelper extends Matchers {

  def executeTypescript(editorName: String, program: String,
                        target: ArtifactSource,
                        params: Map[String, String] = Map(),
                        others: Seq[ProjectOperation] = Nil): ArtifactSource = {

    val cas = {
      val as = SimpleFileBasedArtifactSource(new StringFileArtifact(name = editorName + ".ts", path = ".atomist/editors/" + editorName + ".ts", content = program))
      TypeScriptBuilder.compileWithModel(as)
    }

    val eds = SimpleJavaScriptProjectOperationFinder.find(cas).editors

    if (eds.isEmpty) {
      print(program)
      throw new Exception("No editor was parsed")
    }

    val jsed = eds.head.asInstanceOf[JavaScriptProjectEditor]
    assert(jsed.name === editorName)
    jsed.setContext(others)

    val pe = eds.head
    pe.modify(target, SimpleParameterValues( params)) match {
      case sm: SuccessfulModification =>
        sm.result
      case um =>
        fail("This modification was not successful: " + um)
    }
  }
}
