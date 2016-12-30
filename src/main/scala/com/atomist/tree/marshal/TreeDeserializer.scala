package com.atomist.tree.marshal

import java.util.Collections

import com.atomist.tree.{ContainerTreeNode, SimpleTerminalTreeNode, TreeNode}
import com.atomist.tree.content.text.{AbstractMutableContainerTreeNode, InputPosition, MutableContainerTreeNode, SimpleMutableContainerTreeNode}
import com.fasterxml.jackson.annotation.JsonAnySetter
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper

object TreeDeserializer {

  private val SpecialProperties = Set("nodeId", "type")

  private val mapper = new ObjectMapper() with ScalaObjectMapper
  mapper.registerModule(DefaultScalaModule)

  def fromJson(json: String): ContainerTreeNode = {
    val l = toListOfMaps(json)
    nodeify(l)
  }

  private def toListOfMaps(json: String): List[Map[String, Object]] = {
    val l = mapper.readValue(json, classOf[List[Map[String, Object]]])
    println(mapper.writeValueAsString(l))
    l
  }

  private def nodeify(l: List[Map[String, Object]]): ContainerTreeNode = {
    // Pass 1: Get all nodes individually and put them in a map
    var idToNode: Map[String, LinkableContainerTreeNode] = Map()
    val nodes: Seq[LinkableContainerTreeNode] =
      for {
        m <- l
        if !m.contains("startNodeId")
      } yield {
        val nodeType: String = m.get("type") match {
          case Some(l: Seq[_]) => l.last.toString
          case None => throw new IllegalArgumentException(s"Type is required")
        }
        val nodeName = nodeType
        val simpleFields =
          for {
            k <- m.keys
            if !SpecialProperties.contains(k)
          } yield {
            SimpleTerminalTreeNode(k, m.get(k).toString)
          }
        val ctn = new LinkableContainerTreeNode(nodeName, nodeType, simpleFields.toSeq)
        val nodeId: String = m.getOrElse("nodeId", ???).toString
        idToNode += (nodeId -> ctn)
        ctn
      }

    // Create the linkages
    for {
      m <- l
      if m.contains("startNodeId")
    } {
      val startNodeId: String = m.getOrElse("startNodeId", ???).toString
      val endNodeId: String = m.getOrElse("endNodeId", ???).toString
      val link: String = m.getOrElse("type", ???).toString
      println(s"Creating link from $startNodeId to $endNodeId")
      idToNode.get(startNodeId) match {
        case Some(parent) => parent.link(idToNode.get(endNodeId).getOrElse(???), link)
        case None => ???
      }
    }

    // Return the root node
    nodes.head
  }
}


class LinkableContainerTreeNode(
                                 val nodeName: String,
                                 override val nodeType: String,
                                 private var fieldValues: Seq[TreeNode]
                               )
  extends ContainerTreeNode {

  def link(c: LinkableContainerTreeNode, link: String): Unit = {
    // Add a child with the appropriate name
    val nn = new LinkableContainerTreeNode(link, c.nodeType, c.fieldValues)
    fieldValues = fieldValues :+ nn
  }

  override def childNodeNames: Set[String] =
    fieldValues.map(f => f.nodeName).toSet

  override def childNodeTypes: Set[String] =
    fieldValues.map(f => f.nodeType).toSet

  override def value: String = ???

  override def childrenNamed(key: String): Seq[TreeNode] =
    fieldValues.filter(n => n.nodeName.equals(key))

}