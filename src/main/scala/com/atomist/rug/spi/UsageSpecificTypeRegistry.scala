package com.atomist.rug.spi

/**
  * Decorating TypeRegistry
  * Try to resolve from the new types first, then fallback to the shared delegate.
  * One of these is instantiated per usage.
  *
  * @param delegate
  * @param newTypes
  */
class UsageSpecificTypeRegistry(delegate: TypeRegistry,
                                newTypes: Seq[Typed]) extends TypeRegistry {

  override def findByName(kind: String): Option[Typed] =
    newTypes.find(t => kind.equals(t.name)).orElse(delegate.findByName(kind))

  override def typeNames: Traversable[String] =
    newTypes.map(_.name) ++ delegate.typeNames

  override def types: Seq[Typed] =
    newTypes ++ types
}
