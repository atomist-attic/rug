package com.atomist.project.archive

import com.atomist.project.edit.ProjectEditor
import com.atomist.project.generate.ProjectGenerator
import com.atomist.project.review.ProjectReviewer
import com.atomist.project.{Executor, ProjectOperation}
import com.atomist.rug.RugReferenceException
import com.atomist.rug.runtime.rugdsl.RugDrivenProjectReviewer

import scala.collection.JavaConversions._

object Operations {

  val Empty = Operations(Nil, Nil, Nil, Nil)
}

/**
  * A group of ProjectOperation objects that we can add to other groups.
  */
case class Operations(
                       generators: Seq[ProjectGenerator],
                       editors: Seq[ProjectEditor],
                       reviewers: Seq[ProjectReviewer],
                       executors: Seq[Executor] = Nil) {

  val allOperations: Seq[ProjectOperation] =
    generators ++ editors ++ reviewers ++ executors

  def publishedReviewers: Seq[ProjectReviewer] = reviewers.collect {
    case red: RugDrivenProjectReviewer if red.program.publishedName.isDefined =>
      red
  }

  def +(that: Operations) =
    Operations(generators ++ that.generators,
      editors ++ that.editors,
      reviewers ++ that.reviewers,
      executors ++ that.executors
    )

  def generatorNames: Seq[String] = generators.map(_.name)

  def editorNames: Seq[String] = editors.map(_.name)

  def reviewerNames: Seq[String] = reviewers.map(_.name)

  def executorNames: Seq[String] = executors.map(_.name)

  def findEditor(editorName: String): ProjectEditor = {
    editors.find(ed => editorName.equals(ed.name)).getOrElse(
      throw new RugReferenceException(editorName, s"No editor with name '$editorName': Know of [${editorNames.mkString(",")}]")
    )
  }

  def findGenerator(generatorName: String): ProjectGenerator = {
    generators.find(g => generatorName.equals(g.name)).getOrElse(
      throw new RugReferenceException(generatorName, s"No generator with name '$generatorName': Know of [${generatorNames.mkString(",")}]")
    )
  }

  def findReviewer(reviewerName: String): ProjectReviewer = {
    reviewers.find(r => reviewerName.equals(r.name)).getOrElse(
      throw new RugReferenceException(reviewerName, s"No reviewer with name '$reviewerName': Know of [${reviewerNames.mkString(",")}]")
    )
  }

  def findExecutor(executorName: String): Executor = {
    executors.find(ex => executorName.equals(ex.name)).getOrElse(
      throw new RugReferenceException(executorName, s"No executor with name '$executorName': Know of [${executorNames.mkString(",")}]")
    )
  }

  override def toString = {
    def showOp(op: ProjectOperation): String = {
      s"${op.name} - ${op.parameters.map(_.name).mkString(",")}" +
        (if (op.description.length > op.name.length) s"\n\t\t${op.description}" else "")
    }

    s"Generators are \n\t${generators.map(g => showOp(g)).mkString("\n\t")}\n" +
      s"Editors are \n\t${editors.map(ed => showOp(ed)).mkString("\n\t")}\n" +
      s"Reviewers are ${reviewers.map(r => showOp(r)).mkString("\n\t")}\n" +
      s"Executors are ${executors.map(r => showOp(r)).mkString("\n\t")}\n"
  }
}