package com.atomist.project.archive

import com.atomist.project.edit.ProjectEditor
import com.atomist.project.generate.ProjectGenerator
import com.atomist.project.review.ProjectReviewer
import com.atomist.rug.runtime.js._
import com.atomist.rug.runtime._
import com.atomist.source.ArtifactSource

/**
  * Find all rugs
  */
object RugArchiveReader {

  private val finders: Seq[JavaScriptRugFinder[_ <: Rug]] = Seq(
    new JavaScriptCommandHandlerFinder(),
    new JavaScriptResponseHandlerFinder(),
    new JavaScriptEventHandlerFinder(),
    new JavaScriptProjectReviewerFinder(),
    new JavaScriptProjectGeneratorFinder(),
    new JavaScriptProjectEditorFinder())

  /**
    * Find Rugs of all known kinds (project operations etc) and tell them about one another
    * @param as
    * @param otherRugs - other rugs brought in via manifest deps
    * @return
    */
  def find(as: ArtifactSource, otherRugs: Seq[AddressableRug] = Nil): Rugs = {
    val jsc = new JavaScriptContext(as)
    val handlers = finders.flatMap(finder => finder.find(jsc, otherRugs))

    val rugs = new Rugs(
      handlers.collect{case h: ProjectEditor => h},
      handlers.collect{case h: ProjectGenerator => h},
      handlers.collect{case h: ProjectReviewer => h},
      handlers.collect{case h: CommandHandler => h},
      handlers.collect{case h: EventHandler => h},
      handlers.collect{case h: ResponseHandler=> h}
      )
    //tell the rugs about one another
    rugs.allRugs.foreach(r => r.addToArchiveContext(rugs.allRugs))
    rugs
  }
}
