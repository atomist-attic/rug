package com.atomist.rug.spi

import com.atomist.param.ParameterValidationPatterns

/**
  * Represents the type of an operation parameter or parameter
  */
sealed trait ParameterOrReturnType {

  def name: String

  def isArray: Boolean

}

object SimpleParameterOrReturnType {

  def fromJavaType(t: java.lang.reflect.Type): SimpleParameterOrReturnType = {
    val typeName = t.getTypeName
    typeName.indexOf("<") match {
      case index if index > -1 =>
        val left = typeName.take(index)
        val right = typeName.substring(index + 1).dropRight(1)
        SimpleParameterOrReturnType(right, isArray = true)
      case -1 =>
        SimpleParameterOrReturnType(t.getTypeName, isArray = false)
    }
  }
}

/**
  * Represents a type or reference for which we don't need to generate
  * anything special
  */
case class SimpleParameterOrReturnType(name: String, isArray: Boolean)
  extends ParameterOrReturnType

/**
  * Represents an enum type that we must generate
  */
case class EnumParameterOrReturnType(name: String, legalValues: Seq[String]) extends ParameterOrReturnType {

  override def isArray: Boolean = false

}
