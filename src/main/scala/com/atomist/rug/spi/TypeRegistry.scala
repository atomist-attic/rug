package com.atomist.rug.spi

/**
  * Allows extension to Rug functionality by understanding noun
  * kinds, such as "file", "line" or "class"
  */
trait TypeRegistry {

  def findByName(kind: String): Option[Typed]

  def typeNames: Traversable[String]

  def types: Seq[Typed]
}

class SimpleTypeRegistry(pTypes: Traversable[Typed]) extends TypeRegistry {

  override def findByName(kind: String): Option[Typed] = pTypes.find(_.name == kind)

  override def typeNames: Traversable[String] = pTypes.map(_.name)

  override def types: Seq[Typed] = pTypes.toSeq
}