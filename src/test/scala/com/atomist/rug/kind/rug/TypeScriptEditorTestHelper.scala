package com.atomist.rug.kind.rug

import com.atomist.project.{ProjectOperation, SimpleProjectOperationArguments}
import com.atomist.project.edit.{ProjectEditor, SuccessfulModification}
import com.atomist.rug.compiler.typescript.TypeScriptCompiler
import com.atomist.rug.compiler.typescript.compilation.CompilerFactory
import com.atomist.rug.runtime.js.{JavaScriptInvokingProjectEditor, JavaScriptOperationFinder}
import com.atomist.rug.{CompilerChainPipeline, RugPipeline, TestUtils}
import com.atomist.rug.ts.RugTranspiler
import com.atomist.source.{ArtifactSource, SimpleFileBasedArtifactSource, StringFileArtifact}
import org.scalatest.Matchers

trait TypeScriptEditorTestHelper extends Matchers {

  val typeScriptPipeline: RugPipeline =
    new CompilerChainPipeline(Seq(new RugTranspiler(), new TypeScriptCompiler(CompilerFactory.create())))

  def executeTypescript(editorFilename: String, program: String,
                                      target: ArtifactSource,
                                      params: Map[String, String] = Map(),
                                      others: Seq[ProjectOperation] = Nil)
  : ArtifactSource = {

    val as = SimpleFileBasedArtifactSource(new StringFileArtifact(editorFilename, ".atomist/editors/" + editorName + ".ts", program))
    val cas = TestUtils.compileWithModel(as)

    val eds = JavaScriptOperationFinder.fromJavaScriptArchive(as)

    if (eds.isEmpty) {
      print(program); throw new Exception("No editor was parsed")
    }

    val jsed = eds.head.asInstanceOf[JavaScriptInvokingProjectEditor]
    jsed.name should be(editorName)
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
