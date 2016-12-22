package com.atomist.rug.kind.rug

import com.atomist.project.{ProjectOperation, SimpleProjectOperationArguments}
import com.atomist.project.edit.SuccessfulModification
import com.atomist.rug.runtime.js.{JavaScriptInvokingProjectEditor, JavaScriptOperationFinder}
import com.atomist.source.{ArtifactSource, SimpleFileBasedArtifactSource, StringFileArtifact}
import org.scalatest.Matchers

trait TypeScriptEditorTestHelper extends Matchers {
  def executeTypescript(editorFilename: String, program: String,
                                      target: ArtifactSource,
                                      params: Map[String, String] = Map(),
                                      others: Seq[ProjectOperation] = Nil)
  : ArtifactSource = {
    val as = SimpleFileBasedArtifactSource(new StringFileArtifact(editorFilename, ".atomist/editors/" + editorFilename, program))
    val jsed = JavaScriptOperationFinder.fromJavaScriptArchive(as).head.asInstanceOf[JavaScriptInvokingProjectEditor]
    jsed.modify(target, SimpleProjectOperationArguments("", params)) match {
      case sm: SuccessfulModification =>
        sm.result
      case um =>
        fail("This modification was not successful: " + um)
    }
  }
}
