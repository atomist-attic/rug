package com.atomist.project.archive

import com.atomist.project.ProjectOperation
import com.atomist.project.edit.ProjectEditor
import com.atomist.project.generate.ProjectGenerator
import com.atomist.project.review.ProjectReviewer
import com.atomist.rug.runtime.Rug
import com.atomist.rug.runtime.js._
import com.atomist.source.ArtifactSource

/**
  * Convenience, but expensive as JavaScriptContext is not reused
  * Also namespace/otherops not used. Really only for test/fun.
  */
object SimpleJavaScriptProjectOperationFinder {
  def find(as: ArtifactSource) : Rugs = {
    new JavaScriptProjectOperationFinder(new JavaScriptContext(as)).find(None,Nil)
  }
}

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
    * @param namespace the namespace
    * @param otherRugs other rugs brought in via manifest deps
    */
  def find(namespace: Option[String], otherRugs: Seq[Rug]): Rugs = {
    //TODO make better use of namespace/otherRugs etc.
    val ops = finders.flatMap(finder => finder.find(jsc))
    Rugs(
      ops.collect{case o: ProjectEditor => o},
      ops.collect{case o: ProjectGenerator => o},
      ops.collect{case o: ProjectReviewer => o},
      Nil,
      Nil,
      Nil
    )
  }
}