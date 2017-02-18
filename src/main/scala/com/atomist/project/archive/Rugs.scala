package com.atomist.project.archive

import com.atomist.project.ProjectOperation
import com.atomist.project.edit.ProjectEditor
import com.atomist.project.generate.ProjectGenerator
import com.atomist.project.review.ProjectReviewer
import com.atomist.rug.runtime._

/**
  * Convenience wrapper to hold the different Rug types
  */
case class Rugs(
                editors: Seq[ProjectEditor],
                generators: Seq[ProjectGenerator],
                reviewers: Seq[ProjectReviewer],
                commandHandlers: Seq[CommandHandler],
                eventHandlers: Seq[EventHandler],
                responseHandlers: Seq[ResponseHandler]
               ){

  def projectOperations: Seq[ProjectOperation] = (editors ++ generators ++ reviewers).asInstanceOf[Seq[ProjectOperation]]

  def editorNames: Seq[String] = names(editors)
  def generatorNames: Seq[String] = names(generators)
  def reviewerNames: Seq[String] = names(reviewers)
  def commandHandlerNames: Seq[String] = names(commandHandlers)
  def eventHandlerNames: Seq[String] = names(eventHandlers)
  def responseHandlerNames: Seq[String] = names(responseHandlers)

  private def names(rugs: Seq[Rug]): Seq[String] = rugs.map(r => r.name)

  override def toString: String = {
    def showOp(rug: Rug): String = {
      val str: String = rug match {
          //how do I make the first two use the same case?
        case op: ProjectOperation =>
          s"${op.name} - ${op.parameters.map(_.name).mkString(",")}"
        case op: ResponseHandler =>
          s"${op.name} - ${op.parameters.map(_.name).mkString(",")}"
        case op: CommandHandler =>
          s"${op.name} - ${op.parameters.map(_.name).mkString(",") + s" Intent: ${op.intent.mkString(",")}"}"
        case op: EventHandler=>
          s"${op.name} - Root node: ${op.rootNodeName}"
        case _ => ???
      }
      if (rug.description.length > rug.name.length) s"$str\n\t\t${rug.description}" else str
    }

      s"Generators are \n\t${generators.map(g => showOp(g)).mkString("\n\t")}\n" +
      s"Editors are \n\t${editors.map(ed => showOp(ed)).mkString("\n\t")}\n" +
      s"Reviewers are ${reviewers.map(r => showOp(r)).mkString("\n\t")}\n" +
      s"Command Handlers are ${commandHandlers.map(r => showOp(r)).mkString("\n\t")}\n" +
      s"Response Handlers are ${responseHandlers.map(r => showOp(r)).mkString("\n\t")}\n" +
      s"Event Handlers are ${eventHandlers.map(r => showOp(r)).mkString("\n\t")}\n"
  }
}
