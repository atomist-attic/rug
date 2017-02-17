package com.atomist.project.archive

import com.atomist.rug.runtime.js._
import com.atomist.rug.runtime.js.interop.JavaScriptHandlerContext
import com.atomist.rug.runtime.{CommandHandler, EventHandler, ResponseHandler, Rug}
import com.atomist.source.ArtifactSource

/**
  * Find all JavaScript based rugs
  */
class JavaScriptRugArchiveReader(ctx: JavaScriptHandlerContext)
  extends RugArchiveReader[Rug] {

  private val finders: Seq[JavaScriptRugFinder[_ <: Rug]] = Seq(
    new JavaScriptCommandHandlerFinder(ctx),
    new JavaScriptResponseHandlerFinder(ctx),
    new JavaScriptEventHandlerFinder(ctx))

  /**
    * Find JS based Rugs of all known kinds (project operations etc)
    * @param as
    * @param namespace
    * @param otherRugs - other rugs brought in via manifest deps
    * @return
    */
  override def find(as: ArtifactSource, namespace: Option[String], otherRugs: Seq[Rug]): Rugs = {
    val jsc = new JavaScriptContext(as)
    val jsReader = new JavaScriptProjectOperationFinder(jsc)
    //TODO - make better use of namespace/otherRugs for early validation!
    val handlers = finders.flatMap(finder => finder.find(jsc))
    val ops = jsReader.find(namespace, otherRugs)

    Rugs(
      ops.editors,
      ops.generators,
      ops.reviewers,
      handlers.collect{case h: CommandHandler => h},
      handlers.collect{case h: EventHandler => h},
      handlers.collect{case h: ResponseHandler=> h}
      )
  }
}
