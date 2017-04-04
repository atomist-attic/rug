package com.atomist.rug.spi

/**
  * Represents the type of an operation parameter or parameter
  */
sealed trait ParameterOrReturnType {

  def name: String

  def isArray: Boolean

}

/**
  * Represents a type or reference for which we don't need to generate
  * anything special
  */
case class SimpleParameterOrReturnType(name: String, isArray: Boolean = false) extends ParameterOrReturnType

/**
  * Represents an enum type that we must generate
  */
case class EnumParameterOrReturnType(name: String, legalValues: Seq[String]) extends ParameterOrReturnType {

  override def isArray: Boolean = false

}
