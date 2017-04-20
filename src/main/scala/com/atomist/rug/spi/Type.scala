package com.atomist.rug.spi

import com.atomist.graph.GraphNode
import com.atomist.rug.kind.dynamic.ChildResolver

/**
  * Support for a new Rug "kind" or "type" that can be used in path expressions
  * and resolved in a context.
  */
abstract class Type
  extends ChildResolver
    with Typed {

  /**
    * Describe the GraphNode or MutableView subclass to allow for reflective function export
    */
  def runtimeClass: Class[_ <: GraphNode]

}
