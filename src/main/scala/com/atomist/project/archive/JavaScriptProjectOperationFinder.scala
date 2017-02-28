package com.atomist.project.archive

import com.atomist.project.ProjectOperation
import com.atomist.project.edit.ProjectEditor
import com.atomist.project.generate.ProjectGenerator
import com.atomist.project.review.ProjectReviewer
import com.atomist.rug.runtime.AddressableRug
import com.atomist.rug.runtime.js._
import com.atomist.source.ArtifactSource

/**
  * Find ProjectOperations in an artifact source
  * And we need this in the rug DSL pipeline :/
  */
class JavaScriptProjectOperationFinder(jsc: JavaScriptContext) {

  private val finders: Seq[JavaScriptRugFinder[_ <: ProjectOperation]] = Seq(
    new JavaScriptProjectReviewerFinder(),
    new JavaScriptProjectGeneratorFinder(),
    new JavaScriptProjectEditorFinder()
  )

  /**
    *  Finds rugs.
    *
    * @param otherRugs other rugs brought in via manifest deps
    */
  def find(otherRugs: Seq[AddressableRug]): Rugs = {
    val ops = finders.flatMap(finder => finder.find(jsc, otherRugs))
    val rugs = Rugs(
      ops.collect{case o: ProjectEditor => o},
      ops.collect{case o: ProjectGenerator => o},
      ops.collect{case o: ProjectReviewer => o},
      Nil,
      Nil,
      Nil
    )
    //tell the rugs about one another
    rugs.allRugs.foreach(r => r.addToArchiveContext(rugs.allRugs))
    rugs
  }
}
