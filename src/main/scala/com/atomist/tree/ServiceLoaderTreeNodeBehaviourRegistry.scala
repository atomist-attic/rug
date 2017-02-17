package com.atomist.tree

import java.util.ServiceLoader

import com.atomist.graph.GraphNode
import com.atomist.rug.RugRuntimeException
import com.atomist.rug.spi.{TreeNodeBehaviour, TreeNodeBehaviourRegistry}
import com.typesafe.scalalogging.LazyLogging

import scala.collection.JavaConversions
import scala.collection.JavaConverters._

class ServiceLoaderTreeNodeBehaviourRegistry extends TreeNodeBehaviourRegistry with LazyLogging {

  private lazy val behaviours: Seq[TreeNodeBehaviour[GraphNode]] = {
    ServiceLoader.load(classOf[TreeNodeBehaviour[_]]).asScala.map {
      case c: TreeNodeBehaviour[GraphNode @unchecked] =>
        logger.info(s"Registered Tree Node Behaviour '${c.name}'")
        c
      case wtf =>
        throw new RugRuntimeException("Tree Node Behaviour", s"Class ${wtf.getClass} must implement TreeNodeBehaviour interface", null)
    }
  }.toSeq

  override def findByNodeAndName(treeNode: GraphNode, name: String): Option[TreeNodeBehaviour[GraphNode]] = {
    val nodeTypes = treeNode.nodeTags
    val candidates = behaviours.filter(c => c.name == name && JavaConversions.asScalaSet(c.nodeTypes).exists(t => nodeTypes.contains(t)))
    candidates.length match {
      case 1 => Option(candidates.head)
      case 0 => None
      case x =>
        throw new RugRuntimeException("Tree Node Behaviour", s"Multiple TreeNodeBehaviours $x registered for '$name' on treeNode '${treeNode.nodeTags}'")
    }
  }
}
