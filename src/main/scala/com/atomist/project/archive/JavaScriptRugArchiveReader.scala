package com.atomist.project.archive

import com.atomist.rug.runtime.js._
import com.atomist.rug.runtime._
import com.atomist.source.ArtifactSource

/**
  * Find all JavaScript based rugs
  */
class JavaScriptRugArchiveReader
  extends RugArchiveReader {

  private val finders: Seq[JavaScriptRugFinder[_ <: Rug]] = Seq(
    new JavaScriptCommandHandlerFinder(),
    new JavaScriptResponseHandlerFinder(),
    new JavaScriptEventHandlerFinder())

  /**
    * Find JS based Rugs of all known kinds (project operations etc)
    * @param as
    * @param otherRugs - other rugs brought in via manifest deps
    * @return
    */
  override def find(as: ArtifactSource, otherRugs: Seq[AddressableRug]): Rugs = {
    val jsc = new JavaScriptContext(as)
    val jsReader = new JavaScriptProjectOperationFinder(jsc)
    val handlers = finders.flatMap(finder => finder.find(jsc, otherRugs))
    val ops = jsReader.find(otherRugs)

    val rugs = Rugs(
      ops.editors,
      ops.generators,
      ops.reviewers,
      handlers.collect{case h: CommandHandler => h},
      handlers.collect{case h: EventHandler => h},
      handlers.collect{case h: ResponseHandler=> h}
      )
    //tell the rugs about one another
    rugs.allRugs.foreach(r => r.addToArchiveContext(rugs.allRugs))
    rugs
  }
}
