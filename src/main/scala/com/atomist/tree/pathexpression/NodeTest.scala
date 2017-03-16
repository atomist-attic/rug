package com.atomist.tree.pathexpression

import com.atomist.graph.GraphNode
import com.atomist.rug.spi.TypeRegistry
import com.atomist.tree.pathexpression.ExecutionResult.ExecutionResult
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeInfo.{As, Id}

/**
  * One of the three core elements of a LocationStep. Inspired by XPath NodeTest.
  */
@JsonTypeInfo(include = As.WRAPPER_OBJECT, use = Id.NAME)
trait NodeTest {

  /**
    * Find nodes from the given node, observing the given AxisSpecifier
    *
    * @param tn   node to drill down from
    * @param axis AxisSpecifier indicating the kind of navigation
    * @return Resulting nodes
    */
  def follow(tn: GraphNode, axis: AxisSpecifier, ee: ExpressionEngine, typeRegistry: TypeRegistry): ExecutionResult

}


