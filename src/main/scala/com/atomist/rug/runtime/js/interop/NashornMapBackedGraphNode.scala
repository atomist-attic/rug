package com.atomist.rug.runtime.js.interop

import java.util.Objects

import com.atomist.graph.GraphNode
import com.atomist.tree.SimpleTerminalTreeNode
import jdk.nashorn.api.scripting.ScriptObjectMirror

import scala.collection.JavaConverters._

object NashornMapBackedGraphNode {

  /**
    * Convert this object returned from Nashorn to a GraphNode if possible. Only take
    * account of properties
    */
  def toGraphNode(nashornReturn: Object): Option[GraphNode] = nashornReturn match {
    case som: ScriptObjectMirror =>
      Some(new NashornMapBackedGraphNode(som, new NodeRegistry()))
    case _ =>
      None
  }

  private val NodeIdField = "nodeId"
  private val NodeNameField = "nodeName"
  private val NodeTagsField = "nodeTag"
  private val NodeRefField = "nodeRef"

}

/**
  * Mutable registry of nodes we've seen
  */
private[interop] class NodeRegistry {

  private var reg: Map[String, NashornMapBackedGraphNode] = Map()

  def register(gn: NashornMapBackedGraphNode): Unit = {
    gn.nodeId.foreach(id => {
      reg = reg ++ Map(id -> gn)
    })
  }

  def get(id: String): Option[NashornMapBackedGraphNode] = reg.get(id)

  override def toString: String = s"NodeRegistry ${hashCode()}: Known ids=[${reg.keySet.mkString(",")}]"

}

/**
  * Backed by a Map that can include simple properties or Nashorn ScriptObjectMirror in the event of nesting.
  * Handles cycles if references are provided.
  */
private class NashornMapBackedGraphNode(som: ScriptObjectMirror,
                                        nodeRegistry: NodeRegistry) extends GraphNode {

  import NashornMapBackedGraphNode._

  private val relevantPropertiesAndValues =
    (NashornUtils.extractProperties(som) ++
    NashornUtils.extractNoArgFunctions(som))
      // Null properties don't match path expressions, so suppress them
      .filter(_._2 != null)

  val nodeId: Option[String] = relevantPropertiesAndValues.get(NodeIdField).map(id => "" + id)
  nodeRegistry.register(this)

  override def nodeName: String = relevantPropertiesAndValues.get(NodeNameField) match {
    case None => ""
    case Some(s: String) => s
    case x => Objects.toString(x)
  }

  override def nodeTags: Set[String] = {
    relevantPropertiesAndValues("nodeTags") match {
      case som: ScriptObjectMirror if som.isArray =>
        som.values().asScala.map(Objects.toString(_)).toSet
      case _ => Set()
    }
  }

  override lazy val relatedNodes: Seq[GraphNode] =
    relevantPropertiesAndValues.keySet.filter(!Set(NodeNameField, NodeTagsField, NodeIdField).contains(_)).flatMap(toNode).toSeq

  override lazy val relatedNodeNames: Set[String] = relatedNodes.map(_.nodeName).toSet

  override def relatedNodeTypes: Set[String] = relatedNodes.flatMap(_.nodeTags).toSet

  override def relatedNodesNamed(name: String): Seq[GraphNode] =
    relatedNodes.filter(_.nodeName == name)

  override def followEdge(name: String): Seq[GraphNode] =
    toNode(name)

  private def toNode(key: String): Seq[GraphNode] = {
    def nodify(som: ScriptObjectMirror): GraphNode = {
      if (NashornUtils.hasDefinedProperties(som, NodeRefField)) {
        // It's a ref to an existing node. We must have seen that node before
        val id = NashornUtils.stringProperty(som, NodeRefField)
        nodeRegistry.get(id) match {
          case None => throw new IllegalArgumentException(s"No node found with referenced id [$id] in $nodeRegistry")
          case Some(n) => n
        }
      }
      else {
        new NashornMapBackedGraphNode(som, nodeRegistry)
      }
    }

    relevantPropertiesAndValues.get(key) match {
      case None => Nil
      case Some(s: String) => Seq(SimpleTerminalTreeNode(key, s, Set()))
      case Some(som: ScriptObjectMirror) if som.isArray =>
        som.values().asScala.collect {
          case s: ScriptObjectMirror => nodify(s)
        }.toSeq
      case Some(som: ScriptObjectMirror) =>
        Seq(nodify(som))
      case x =>
        val v = Objects.toString(x)
        Seq(SimpleTerminalTreeNode(key, v, Set()))
    }
  }

  override def toString: String = s"${getClass.getSimpleName}: name=[$nodeName],id=$nodeId"

}
