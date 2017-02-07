package com.atomist.event.archive

import com.atomist.plan.TreeMaterializer
import com.atomist.project.archive.{AtomistConfig, DefaultAtomistConfig}
import com.atomist.rug.runtime.SystemEvent
import com.atomist.rug.runtime.js.JavaScriptHandlerFinder
import com.atomist.rug.runtime.js.interop.{JavaScriptEventHandlerTest, JavaScriptHandlerContext}
import com.atomist.rug.ts.TypeScriptBuilder
import com.atomist.source.SimpleFileBasedArtifactSource
import com.atomist.tree.TreeNode
import com.atomist.tree.pathexpression.PathExpression
import org.scalatest.{FlatSpec, Matchers}

class HandlerArchiveReaderTest extends FlatSpec with Matchers {

  val atomistConfig: AtomistConfig = DefaultAtomistConfig
  val treeMaterializer: TreeMaterializer = TestTreeMaterializer

  it should "parse single new-style handler" in {

    val rugArchive = TypeScriptBuilder.compileWithModel(SimpleFileBasedArtifactSource(JavaScriptEventHandlerTest.reOpenCloseIssueProgram))
    val handlers = JavaScriptHandlerFinder.findEventHandlers(rugArchive, new JavaScriptHandlerContext("XX", treeMaterializer))
    handlers.size should be(1)
    handlers.head.rootNodeName should be("issue")
  }

  object TestTreeMaterializer extends TreeMaterializer {

    override def rootNodeFor(e: SystemEvent, pe: PathExpression): TreeNode = ???

    override def hydrate(teamId: String, rawRootNode: TreeNode, pe: PathExpression): TreeNode = ???
  }
}
