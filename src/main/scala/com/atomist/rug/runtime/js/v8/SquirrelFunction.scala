package com.atomist.rug.runtime.js.v8

import java.util.Objects

import com.atomist.graph.GraphNode
import com.atomist.rug.spi.{Type, TypeRegistry}
import com.eclipsesource.v8.{JavaCallback, V8Array, V8Object}

/**
  * Implementation of squirrel function
  */
class SquirrelFunction(node: NodeWrapper, gn: GraphNode, array: Boolean, typeRegistry: TypeRegistry) extends JavaCallback{

  override def invoke(receiver: V8Object, parameters: V8Array): AnyRef = {
    val typeName = Objects.toString(parameters.get(0))
    val rawSeq = (typeRegistry.findByName(typeName) match {
      case Some(t: Type) =>
        t.findAllIn(gn)
      case Some(_) =>
        throw new IllegalArgumentException(s"Attempt to resolve type [$typeName], which does not support resolution under a GraphNode")
      case None =>
        throw new IllegalArgumentException(s"Attempt to resolve type [$typeName], which is not registered")
    }).getOrElse(Nil)
    if (array)
      Proxy.ifNeccessary(node, rawSeq)
    else {
      require(rawSeq.size < 2, s"Needed 0 or 1 returns for type [$typeName] under $this, got ${rawSeq.size}. Call $$jumpInto to get an array")
      Proxy.ifNeccessary(node, rawSeq.headOption.orNull)
    }
  }
}
