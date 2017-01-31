package com.atomist.event.archive

import com.atomist.event.SystemEventHandler
import com.atomist.plan.TreeMaterializer
import com.atomist.project.ProjectOperation
import com.atomist.project.archive.{AtomistConfig, DefaultAtomistConfig}
import com.atomist.rug.BadRugException
import com.atomist.rug.kind.DefaultTypeRegistry
import com.atomist.rug.kind.dynamic.{DefaultViewFinder, ViewFinder}
import com.atomist.rug.kind.service.MessageBuilder
import com.atomist.rug.runtime.js.JavaScriptHandlerFinder
import com.atomist.rug.runtime.js.interop.{JavaScriptHandlerContext, jsModelBackedAtomistFacade}
import com.atomist.rug.runtime.rugdsl.DefaultEvaluator
import com.atomist.rug.spi.TypeRegistry
import com.atomist.source.ArtifactSource
import com.atomist.tree.pathexpression.PathExpressionEngine

/**
  * Class that knows how to find SystemEventHandler definitions
  * in a RugArchive and create them.
  */
class HandlerArchiveReader(
                     treeMaterializer: TreeMaterializer,
                     atomistConfig: AtomistConfig = DefaultAtomistConfig,
                     evaluator: DefaultEvaluator = DefaultEvaluator,
                     viewFinder: ViewFinder = DefaultViewFinder,
                     typeRegistry: TypeRegistry = DefaultTypeRegistry) {

  private val pex = new PathExpressionEngine

  @throws[BadRugException]
  @throws[IllegalArgumentException]
  def handlers(
                teamId: String,
                rugArchive: ArtifactSource,
                namespace: Option[String],
                knownOperations: Seq[ProjectOperation] = Nil,
                messageBuilder: MessageBuilder): Seq[SystemEventHandler] = {
    val atomist = new jsModelBackedAtomistFacade(teamId, messageBuilder, treeMaterializer)
    JavaScriptHandlerFinder.registerHandlers(rugArchive, atomist)
    val handlers = atomist.handlers
    if (handlers.nonEmpty) {
      handlers
    } else {
      JavaScriptHandlerFinder.fromJavaScriptArchive(rugArchive, new JavaScriptHandlerContext(teamId, treeMaterializer, messageBuilder))
    }
  }
}
