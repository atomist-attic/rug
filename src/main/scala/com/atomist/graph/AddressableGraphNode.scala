package com.atomist.graph

/**
  * Node that knows its address in graph or tree
  */
trait AddressableGraphNode extends GraphNode {

  def address: String
}
