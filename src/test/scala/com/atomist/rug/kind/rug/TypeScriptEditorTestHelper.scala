package com.atomist.rug.kind.rug

import com.atomist.project.edit.{ProjectEditor, SuccessfulModification}
import com.atomist.project.{ProjectOperation, SimpleProjectOperationArguments}
import com.atomist.rug.runtime.js.{JavaScriptInvokingProjectEditor, JavaScriptOperationFinder}
import com.atomist.rug.ts.{RugTranspiler, TypeScriptBuilder}
import com.atomist.rug.{CompilerChainPipeline, RugPipeline}
import com.atomist.source.{ArtifactSource, SimpleFileBasedArtifactSource, StringFileArtifact}
import org.scalatest.Matchers

trait TypeScriptEditorTestHelper extends Matchers {

  val typeScriptPipeline: RugPipeline =
    new CompilerChainPipeline(Seq(new RugTranspiler(), TypeScriptBuilder.compiler))

  def executeTypescript(editorName: String, program: String,
                        target: ArtifactSource,
                        params: Map[String, String] = Map(),
                        others: Seq[ProjectOperation] = Nil)
  : ArtifactSource = {

    val cas = {
      val as = SimpleFileBasedArtifactSource(new StringFileArtifact(name = editorName + ".ts", path = ".atomist/editors/" + editorName + ".ts", content = program))
      TypeScriptBuilder.compileWithModel(as)
    }

    val eds = JavaScriptOperationFinder.fromJavaScriptArchive(cas)

    if (eds.isEmpty) {
      print(program);
      throw new Exception("No editor was parsed")
    }

    val jsed = eds.head.asInstanceOf[JavaScriptInvokingProjectEditor]
    assert(jsed.name === editorName)
    jsed.setContext(others)

    val pe = eds.head.asInstanceOf[ProjectEditor]
    pe.modify(target, SimpleProjectOperationArguments("", params)) match {
      case sm: SuccessfulModification =>
        sm.result
      case um =>
        fail("This modification was not successful: " + um)
    }
  }

}
