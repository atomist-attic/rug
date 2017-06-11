package com.atomist.rug.runtime.js.interop

import java.util.Objects

import com.atomist.graph.{AddressableGraphNode, GraphNode}
import com.atomist.rug.runtime.js.{JavaScriptObject, UNDEFINED}
import com.atomist.tree.SimpleTerminalTreeNode

object NashornMapBackedGraphNode {

  /**
    * Convert this object returned from Nashorn to a GraphNode if possible.
    * Will return AddressedGraphNode if address is known.
    */
  def toGraphNode(nashornReturn: Object, nodeRegistry: NodeRegistry = new NodeRegistry): Option[GraphNode] = {
    val result = nashornReturn match {
      case som: JavaScriptObject if nodeRegistry.alreadyWrapped(som).isDefined =>
        nodeRegistry.alreadyWrapped(som)
      case som: JavaScriptObject =>
        val relevantPropertiesAndValues: Map[String, Object] =
          relevantPropertyValues(som)
        relevantPropertiesAndValues.get(NodeAddressField) match {
          case None =>
            Some(new NashornMapBackedGraphNode(som, nodeRegistry))
          case Some(address: String) =>
            Some(new NashornMapBackedAddressableGraphNode(som, nodeRegistry, address))
          case x =>
            val address = Objects.toString(x)
            Some(new NashornMapBackedAddressableGraphNode(som, nodeRegistry, address))
        }
      case _ =>
        None
    }
    result
  }

  private[interop] def relevantPropertyValues(som: JavaScriptObject): Map[String, Object] = {
    (som.extractProperties() ++
      som.extractNoArgFunctionValues())
      // Null properties don't match path expressions, so suppress them
      .filter(_._2 != null)
  }

  val NodeIdField = "nodeId"
  val NodeNameField = "nodeName"
  val NodeAddressField = "address"
  val NodeTagsField = "nodeTag"
  val NodeRefField = "nodeRef"

}

/**
  * Mutable registry of nodes we've seen. We keep track of them by ID
  * and by ScriptObjectMirror backing object, to avoid duplication.
  */
private[interop] class NodeRegistry {

  private var reg: Map[String, NashornMapBackedGraphNode] = Map()
  private var somReg: Map[JavaScriptObject, NashornMapBackedGraphNode] = Map()

  def register(gn: NashornMapBackedGraphNode): Unit = {
    gn.nodeId.foreach(id => {
      reg = reg ++ Map(id -> gn)
    })
    somReg = somReg ++ Map(gn.scriptObject -> gn)
  }

  def get(id: String): Option[NashornMapBackedGraphNode] = reg.get(id)

  def alreadyWrapped(som: JavaScriptObject): Option[NashornMapBackedGraphNode] = somReg.get(som)

  override def toString: String = s"NodeRegistry ${hashCode()}: Known ids=[${reg.keySet.mkString(",")}]"

}

import com.atomist.rug.runtime.js.interop.NashornMapBackedGraphNode._

/**
  * Object used within the JVM whose implementation of the GraphNode interface is based on
  * navigation of a graph defined in JavaScript object or data structure.
  * Backed by a Map that can include simple properties or Nashorn ScriptObjectMirror in the event of nesting.
  * Handles cycles if references are provided.
  */
class NashornMapBackedGraphNode(val scriptObject: JavaScriptObject,
                                nodeRegistry: NodeRegistry)
  extends GraphNode
    with JsonableProxy {

  protected val relevantPropertiesAndValues: Map[String, AnyRef] = relevantPropertyValues(scriptObject)

  val nodeId: Option[String] = relevantPropertiesAndValues.get(NodeIdField).map(id => "" + id)
  nodeRegistry.register(this)

  private val traversableEdges: Map[String, Seq[GraphNode]] =
    relevantPropertiesAndValues.keys
      .map(key => (key, toNode(key)))
      .toMap

  override def nodeName: String = relevantPropertiesAndValues.get(NodeNameField) match {
    case None => ""
    case Some(s: String) => s
    case x => Objects.toString(x)
  }

  override def nodeTags: Set[String] = {
    relevantPropertiesAndValues.get("nodeTags") match {
      case Some(som: JavaScriptObject) if som.isSeq =>
        som.values().map(Objects.toString(_)).toSet
      case _ => Set()
    }
  }

  override protected def propertyMap: Map[String, Any] = relevantPropertiesAndValues

  override lazy val relatedNodes: Seq[GraphNode] =
    relevantPropertiesAndValues.keySet.diff(Set(NodeNameField, NodeTagsField, NodeIdField))
      .flatMap(toNode)
      .toSeq

  override lazy val relatedNodeNames: Set[String] = relatedNodes.map(_.nodeName).toSet

  override def relatedNodeTypes: Set[String] = relatedNodes.flatMap(_.nodeTags).toSet

  override def relatedNodesNamed(name: String): Seq[GraphNode] =
    (relatedNodes.filter(_.nodeName == name) ++ followEdge(name)).distinct

  override def followEdge(name: String): Seq[GraphNode] =
    traversableEdges.getOrElse(name, Nil)

  private def toNode(key: String): Seq[GraphNode] = {
    def nodify(som: JavaScriptObject): GraphNode = {
      if (som.hasMember(NodeRefField)) {
        // It's a ref to an existing node. We must have seen that node before
        val id = som.stringProperty(NodeRefField)
        nodeRegistry.get(id) match {
          case None => throw new IllegalArgumentException(s"No node found with referenced id [$id] in $nodeRegistry")
          case Some(n) => n
        }
      }
      else {
        toGraphNode(som, nodeRegistry).getOrElse(
          throw new IllegalArgumentException(s"Cannot make graph node from $som")
        )
      }
    }

    relevantPropertiesAndValues.get(key) match {
      case None => Nil
      case Some(s: String) => Seq(SimpleTerminalTreeNode(key, s, Set()))
      case Some(som: JavaScriptObject) if som.isSeq =>
        som.values().collect {
          case s: JavaScriptObject => nodify(s)
        }
      case Some(som: JavaScriptObject) =>
        Seq(nodify(som))
      case Some(UNDEFINED) =>
        Nil
      case Some(x) =>
        val v = Objects.toString(x)
        Seq(SimpleTerminalTreeNode(key, v, Set()))
    }
  }

}

private class NashornMapBackedAddressableGraphNode(
                                                    som: JavaScriptObject,
                                                    nodeRegistry: NodeRegistry,
                                                    val address: String)
  extends NashornMapBackedGraphNode(som, nodeRegistry)
    with AddressableGraphNode
