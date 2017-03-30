package com.atomist.tree.marshal

import com.atomist.rug.ts.Cardinality
import com.atomist.tree.{ContainerTreeNode, SimpleTerminalTreeNode, TreeNode}
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import com.typesafe.scalalogging.LazyLogging

/**
  * Deserialize a tree from JSON.
  */
object LinkedJsonTreeDeserializer extends LazyLogging {

  private val NodeId = "nodeId"
  private val StartNodeId = "startNodeId"
  private val EndNodeId = "endNodeId"
  private val Type = "type"
  private val CardinalityStr = "cardinality"

  // Properties that we handle specially, rather than treating as ordinary subnodes
  private val SpecialProperties = Set(NodeId, Type)

  // Configure this to handle Scala
  private val mapper = new ObjectMapper() with ScalaObjectMapper
  mapper.registerModule(DefaultScalaModule)

  /**
    * Deserialize from JSON.
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
        val nodeTags: Set[String] = m.get(Type) match {
          case Some(l: Seq[_]) => l.map(_.toString).toSet
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
        val ctn = new LinkableContainerTreeNode(nodeName, nodeTags + TreeNode.Dynamic, simpleFields.toSeq)
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
      val cardinality: Cardinality =
        Cardinality(defaultedStringEntry(m, CardinalityStr, Cardinality.One2One))
      val link: String = requiredStringEntry(m, Type)
      logger.debug(s"Creating link from $startNodeId to $endNodeId")
      idToNode.get(startNodeId) match {
        case Some(parent) => parent.link(
          idToNode.getOrElse(endNodeId,
            throw new IllegalArgumentException(s"Cannot link to end node $endNodeId: not found")),
          link,
          cardinality)
        case None =>
          throw new IllegalArgumentException(s"Cannot link to start node $startNodeId: not found")
      }
    }

    if (nodes.nonEmpty) nodes.head else new EmptyLinkableContainerTreeNode
  }

  private def requiredStringEntry(m: Map[String,Any], key: String): String =
    m.get(key) match {
      case None => throw new IllegalArgumentException(s"Property [$key] was required, but not found in map with keys [${m.keySet.mkString(",")}]")
      case Some(s: String) => s
      case Some(x) => x.toString
    }

  private def defaultedStringEntry(m: Map[String,Any], key: String, default: String): String =
    m.get(key) match {
      case None => default
      case Some(s: String) => s
      case Some(x) => x.toString
    }
}
