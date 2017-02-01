package com.atomist.tree.marshal

import com.atomist.tree.{ContainerTreeNode, SimpleTerminalTreeNode, TreeNode}
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import com.typesafe.scalalogging.LazyLogging

/**
  * Deserialize a tree from JSON
  */
object LinkedJsonTreeDeserializer extends LazyLogging {

  private val NodeId = "nodeId"
  private val StartNodeId = "startNodeId"
  private val EndNodeId = "endNodeId"
  private val Type = "type"

  // Properties that we handle specially, rather than treating as ordinary subnodes
  private val SpecialProperties = Set(NodeId, Type)

  // Configure this to handle Scala
  private val mapper = new ObjectMapper() with ScalaObjectMapper
  mapper.registerModule(DefaultScalaModule)

  /**
    * Deserialize from JSON
    */
  def fromJson(json: String): ContainerTreeNode = {
    val l = toListOfMaps(json)
    nodeify(l)
  }

  private def toListOfMaps(json: String): List[Map[String, Object]] = {
    mapper.readValue(json, classOf[List[Map[String, Object]]])
  }

  private def nodeify(l: List[Map[String, Object]]): ContainerTreeNode = {
    // Pass 1: Get all nodes individually and put them in a map
    var idToNode: Map[String, LinkableContainerTreeNode] = Map()
    val nodes: Seq[LinkableContainerTreeNode] =
      for {
        m <- l
        if !m.contains(StartNodeId)
      } yield {
        val nodeType: String = m.get(Type) match {
          case Some(l: Seq[_]) => l.last.toString
          case None => throw new IllegalArgumentException(s"Type is required")
          case _ => ???
        }
        val nodeName = nodeType
        val simpleFields =
          for {
            k <- m.keys
            if !SpecialProperties.contains(k)
          } yield {
            val nodeValue = m.get(k) match {
              case Some(s: String) => s
              case Some(ns) => ns.toString
              case None => null
            }
            SimpleTerminalTreeNode(k, nodeValue)
          }
        val ctn = new LinkableContainerTreeNode(nodeName, Set(nodeType), simpleFields.toSeq)
        val nodeId: String = requiredStringEntry(m, NodeId)
        idToNode += (nodeId -> ctn)
        ctn
      }

    // Create the linkages
    for {
      m <- l
      if m.contains(StartNodeId)
    } {
      val startNodeId: String = requiredStringEntry(m, StartNodeId)
      val endNodeId: String = requiredStringEntry(m, EndNodeId)
      val link: String = requiredStringEntry(m, Type)
      logger.debug(s"Creating link from $startNodeId to $endNodeId")
      idToNode.get(startNodeId) match {
        case Some(parent) => parent.link(
          idToNode.getOrElse(endNodeId,
            throw new IllegalArgumentException(s"Cannot link to end node $endNodeId: not found")),
          link)
        case None =>
          throw new IllegalArgumentException(s"Cannot link to start node $startNodeId: not found")
      }
    }

    // Return the root node
    nodes.head
  }

  private def requiredStringEntry(m: Map[String,Any], key: String): String =
    m.get(key) match {
      case None => throw new IllegalArgumentException(s"Property [$key] was required, but not found in map with keys [${m.keySet.mkString(",")}]")
      case Some(s: String) => s
      case Some(x) => x.toString
    }
}

private class LinkableContainerTreeNode(
                                         val nodeName: String,
                                         override val nodeTags: Set[String],
                                         private var fieldValues: Seq[TreeNode]
                               )
  extends ContainerTreeNode {

  def link(c: LinkableContainerTreeNode, link: String): Unit = {
    // Add a child with the appropriate name
    val nn = new WrappingLinkableContainerTreeNode(c, link)
    fieldValues = fieldValues :+ nn
  }

  override def childNodeNames: Set[String] =
    fieldValues.map(f => f.nodeName).toSet

  override def childNodeTypes: Set[String] =
    fieldValues.flatMap(f => f.nodeTags).toSet

  override def value: String = ???

  override def childrenNamed(key: String): Seq[TreeNode] =
    fieldValues.filter(n => n.nodeName.equals(key))

}

private class WrappingLinkableContainerTreeNode(val wrappedNode: LinkableContainerTreeNode,
                                                override val nodeName: String)
  extends ContainerTreeNode {

  override def value: String = ???

  override def childNodeNames: Set[String] = wrappedNode.childNodeNames

  override def childNodeTypes: Set[String] = wrappedNode.childNodeTypes

  override def childrenNamed(key: String): Seq[TreeNode] = wrappedNode.childrenNamed(key)
}
