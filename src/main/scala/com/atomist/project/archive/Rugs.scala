package com.atomist.project.archive

import com.atomist.project.edit.ProjectEditor
import com.atomist.project.generate.ProjectGenerator
import com.atomist.rug.runtime._

/**
  * Convenience wrapper to hold the different Rug types
  */

object Rugs {
  def Empty: Rugs = {
    new Rugs(Nil, Nil, Nil, Nil, Nil)
  }
}

case class Rugs(
            private val _editors: Seq[ProjectEditor],
            private val _generators: Seq[ProjectGenerator],
            private val _commandHandlers: Seq[CommandHandler],
            private val _eventHandlers: Seq[EventHandler],
            private val _responseHandlers: Seq[ResponseHandler]
          ) {

  //sort them on the wait out
  def editors: Seq[ProjectEditor] = _editors.sortBy(p => p.name)

  def generators: Seq[ProjectGenerator] = _generators.sortBy(p => p.name)

  def commandHandlers: Seq[CommandHandler] = _commandHandlers.sortBy(p => p.name)

  def eventHandlers: Seq[EventHandler] = _eventHandlers.sortBy(p => p.name)

  def responseHandlers: Seq[ResponseHandler] = _responseHandlers.sortBy(p => p.name)

  def editorNames: Seq[String] = names(editors)

  def generatorNames: Seq[String] = names(generators)

  def commandHandlerNames: Seq[String] = names(commandHandlers)

  def eventHandlerNames: Seq[String] = names(eventHandlers)

  def responseHandlerNames: Seq[String] = names(responseHandlers)

  def allRugs: Seq[Rug] = editors ++ generators ++ commandHandlers ++ eventHandlers ++ responseHandlers

  private def names(rugs: Seq[Rug]): Seq[String] = rugs.map(r => r.name)

  override def toString: String = {
    val sb = new StringBuilder
    sb.append("editors: [")
    sb.append(editorNames.mkString(", "))
    sb.append("] generators: [")
    sb.append(generatorNames.mkString(", "))
    sb.append("] event handlers: [")
    sb.append(eventHandlerNames.mkString(", "))
    sb.append("] command handlers: [")
    sb.append(commandHandlerNames.mkString(", "))
    sb.append("] response handlers: [")
    sb.append(responseHandlerNames.mkString(", "))
    sb.append("]")
    sb.toString
  }
}
