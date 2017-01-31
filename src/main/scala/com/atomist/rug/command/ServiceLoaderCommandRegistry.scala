package com.atomist.rug.command

import java.util.ServiceLoader

import com.atomist.rug.RugRuntimeException
import com.atomist.rug.spi.{Command, CommandRegistry}
import com.atomist.tree.TreeNode
import com.typesafe.scalalogging.LazyLogging

import scala.collection.JavaConversions
import scala.collection.JavaConverters._

class ServiceLoaderCommandRegistry extends CommandRegistry with LazyLogging {

  private lazy val commands: Seq[Command[TreeNode]] = {
    ServiceLoader.load(classOf[Command[_]]).asScala.map {
      case c: Command[TreeNode @unchecked] =>
        logger.info(s"Registered command '${c.name}'")
        c
      case wtf =>
        throw new RugRuntimeException("CommandType", s"Command class ${wtf.getClass} must implement Command interface", null)
    }
  }.toSeq

  override def findByNodeAndName(treeNode: TreeNode, name: String): Option[Command[TreeNode]] = {
    val nodeTypes = treeNode.nodeTags
    val candidates = commands.filter(c => c.name == name && JavaConversions.asScalaSet(c.nodeTypes).exists(t => nodeTypes.contains(t)))
    candidates.length match {
      case 1 => Option(candidates.head)
      case 0 => None
      case x =>
        throw new RugRuntimeException("CommandType", s"Multiple Commands $x registered for '$name' on treeNode '${treeNode.nodeTags}'")
    }
  }
}