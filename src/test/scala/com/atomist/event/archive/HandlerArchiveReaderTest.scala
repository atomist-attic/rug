package com.atomist.event.archive

import com.atomist.event.SystemEvent
import com.atomist.plan.TreeMaterializer
import com.atomist.project.archive.{AtomistConfig, DefaultAtomistConfig}
import com.atomist.rug.kind.service.ConsoleMessageBuilder
import com.atomist.rug.runtime.js.interop.NamedJavaScriptEventHandlerTest
import com.atomist.rug.ts.TypeScriptBuilder
import com.atomist.source.{SimpleFileBasedArtifactSource, StringFileArtifact}
import com.atomist.tree.TreeNode
import com.atomist.tree.pathexpression.PathExpression
import org.scalatest.{FlatSpec, Matchers}

class HandlerArchiveReaderTest extends FlatSpec with Matchers {

  val atomistConfig: AtomistConfig = DefaultAtomistConfig
  val treeMaterializer: TreeMaterializer = TestTreeMaterializer

  val FirstHandler = StringFileArtifact(atomistConfig.handlersRoot + "/First.ts",
      s"""
       |import {Atomist} from "@atomist/rug/operations/Handler"
       |import {Project,File} from "@atomist/rug/model/Core"
       |
       |declare var atomist: Atomist  // <= this is for the compiler only
       |
       |atomist.on<Project,File>('/issue', m => {
       |})
      """.stripMargin
  )

  val SecondHandler = StringFileArtifact(atomistConfig.handlersRoot + "/Second.ts",
    s"""
       |import {Atomist} from "@atomist/rug/operations/Handler"
       |import {Project,File} from "@atomist/rug/model/Core"
       |
       |declare var atomist: Atomist  // <= this is for the compiler only
       |
       |atomist.on<Project,File>('/commit', m => {
       |})
      """.stripMargin
  )

  val ThirdHandler = StringFileArtifact(atomistConfig.handlersRoot + "/Third.ts",
    s"""
       |import {Atomist} from "@atomist/rug/operations/Handler"
       |import {Project,File} from "@atomist/rug/model/Core"
       |
       |declare var atomist: Atomist  // <= this is for the compiler only
       |
       |atomist.on<Project,File>('/commit', m => {
       |  let commit = m.root()
       |  var builder = atomist.messageBuilder().regarding(commit).withCorrelationId("id").send()
       |
       |})
      """.stripMargin
  )

  it should "parse single handler" in {
    val har = new HandlerArchiveReader(treeMaterializer, atomistConfig)
    val handlers = har.handlers("XX", TypeScriptBuilder.compileWithModel(new SimpleFileBasedArtifactSource("", FirstHandler)), None, Nil,
      new ConsoleMessageBuilder("XX", null))
    assert(handlers.size === 1)
  }

  it should "parse two handlers" in {
    val har = new HandlerArchiveReader(treeMaterializer, atomistConfig)
    val handlers = har.handlers("XX", TypeScriptBuilder.compileWithModel(new SimpleFileBasedArtifactSource("", Seq(FirstHandler, SecondHandler))), None, Nil,
      new ConsoleMessageBuilder("XX", null))
    assert(handlers.size === 2)
  }

  it should "parse single new-style handler" in {
    val har = new HandlerArchiveReader(treeMaterializer, atomistConfig)
    val handlers = har.handlers("XX", TypeScriptBuilder.compileWithModel(SimpleFileBasedArtifactSource(NamedJavaScriptEventHandlerTest.reOpenCloseIssueProgram,NamedJavaScriptEventHandlerTest.issuesStuff)), None, Nil,
      new ConsoleMessageBuilder("XX", null))
    assert(handlers.size === 1)
  }

  it should "allow a correlationId to be set" in {
    val har = new HandlerArchiveReader(treeMaterializer, atomistConfig)
    val messageBuilder = new ConsoleMessageBuilder("XX", null)
    val handlers = har.handlers("XX", TypeScriptBuilder.compileWithModel(new SimpleFileBasedArtifactSource("", ThirdHandler)), None, Nil, messageBuilder)
    assert(handlers.size === 1)
  }

  object TestTreeMaterializer extends TreeMaterializer {

    override def rootNodeFor(e: SystemEvent, pe: PathExpression): TreeNode = ???

    override def hydrate(teamId: String, rawRootNode: TreeNode, pe: PathExpression): TreeNode = ???
  }
}
