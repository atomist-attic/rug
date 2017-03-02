package com.atomist.rug.spi

import com.atomist.rug.kind.dynamic.ChildResolver

/**
  * Support for a new Rug "kind" or "type" that can be used in `with` or `from` comprehensions, such
  * as a Java class or Elm module.
  * When kinds are nested, the context should be the mutable view of
  * the outer kind.
  */
abstract class Type
  extends ChildResolver
    with Typed {

  /** Describe the MutableView subclass to allow for reflective function export */
  def runtimeClass: Class[_]
}
