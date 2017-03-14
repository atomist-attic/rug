package com.atomist.rug.spi

/**
  * Parameter to a Rug type.
  */
case class TypeParameter(
                          name: String,
                          parameterType: String,
                          description: Option[String]
                        ) {

  def getDescription: String = description.getOrElse("")

  override def toString: String =
    s"$name : $parameterType : ${description.getOrElse("No Description")}"
}