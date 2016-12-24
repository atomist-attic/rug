package com.atomist.project.archive

import com.atomist.project.edit.ProjectEditor
import com.atomist.project.generate.ProjectGenerator
import com.atomist.project.review.ProjectReviewer
import com.atomist.project.{Executor, ProjectOperation}
import com.atomist.rug.RugReferenceException
import com.atomist.rug.runtime.rugdsl.RugDrivenProjectReviewer

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

  def generatorNames: Seq[String] = generators.map(_.name)

  def editorNames: Seq[String] = editors.map(_.name)

  def reviewerNames: Seq[String] = reviewers.map(_.name)

  def executorNames: Seq[String] = executors.map(_.name)

  override def toString: String = {
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