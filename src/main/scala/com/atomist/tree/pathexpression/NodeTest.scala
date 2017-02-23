package com.atomist.tree.pathexpression

import com.atomist.graph.GraphNode
import com.atomist.rug.spi.TypeRegistry
import com.atomist.tree.AddressableTreeNode
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

/**
  * Convenience methods for NodeTest results
  */
object NodeTest {

  /**
    * Deduplicate a sequence of results
    * We may have duplicates in the found collection because, for example,
    * we might find the Java() node SomeClass.java under the directory "/src"
    * and under "/src/main" and under the actual file.
    * So check that our returned types have distinct backing objects
    */
  def dedupe(results: Seq[GraphNode]): Seq[GraphNode] = {
    var nodeAddressesSeen: Set[String] = Set()
    var toReturn = List.empty[GraphNode]
    results.distinct.foreach {
      case mv: AddressableTreeNode =>
        if (!nodeAddressesSeen.contains(mv.address)) {
          nodeAddressesSeen = nodeAddressesSeen + mv.address
          toReturn = toReturn :+ mv
        }
      case n =>
        // There's no concept of an address here, so we have to take on trust that
        // we didn't find this thing > once
        toReturn = toReturn :+ n
    }
    toReturn
  }
}

