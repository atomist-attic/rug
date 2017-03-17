package com.atomist.tree.pathexpression

import com.atomist.graph.{AddressableGraphNode, GraphNode}

/**
  * Result type and utility methods of a node navigation.
  */
object ExecutionResult {

  type ExecutionResult = Either[String, Seq[GraphNode]]

  def apply(nodes: Seq[GraphNode]): ExecutionResult =
    Right(dedupe(nodes))

  val empty: ExecutionResult = Right(Nil)

  def show(er: ExecutionResult): String = er match {
    case Right(nodes) => s"\t${nodes.map(show).mkString("\n\t")}"
    case Left(err) => s"[$err]"
  }

  def show(n: GraphNode): String = {
    s"${n.nodeName}:${n.nodeTags}"
  }

  /**
    * Deduplicate a sequence of results
    * We may have duplicates in the found collection because, for example,
    * we might find the Java() node SomeClass.java under the directory "/src"
    * and under "/src/main" and under the actual file.
    * So check that our returned types have distinct addresses
    */
  private def dedupe(results: Seq[GraphNode]): Seq[GraphNode] = {
    var nodeAddressesSeen: Set[String] = Set()
    var toReturn = List.empty[GraphNode]
    results.distinct.foreach {
      case gn: AddressableGraphNode if gn.address != null =>
        if (!nodeAddressesSeen.contains(gn.address)) {
          nodeAddressesSeen = nodeAddressesSeen + gn.address
          toReturn = toReturn :+ gn
        }
      case n =>
        // There's no concept of an address here, so we have to take on trust that
        // we didn't find this thing > once
        toReturn = toReturn :+ n
    }
    toReturn
  }

}
