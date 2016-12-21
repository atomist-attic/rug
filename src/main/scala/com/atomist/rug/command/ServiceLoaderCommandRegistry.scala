package com.atomist.rug.command

import java.util.ServiceLoader

import com.atomist.rug.RugRuntimeException
import com.atomist.rug.spi.{Command, CommandRegistry}
import com.atomist.tree.TreeNode
import com.typesafe.scalalogging.LazyLogging

import scala.collection.JavaConverters._

class ServiceLoaderCommandRegistry extends CommandRegistry with LazyLogging {

  private lazy val commandsMap: Map[KeyedCommand, Command[TreeNode]] = {
    ServiceLoader.load(classOf[Command[_]]).asScala.map {
      case c: Command[TreeNode @unchecked] =>
        logger.info(s"Registered command '${c.name}'")
        KeyedCommand(c.`type`, c.name) -> c
      case wtf =>
        throw new RugRuntimeException("CommandType", s"Command class ${wtf.getClass} must implement Command interface", null)
    }
  }.toMap

  override def findByNodeAndName(treeNode: TreeNode, name: String): Option[Command[TreeNode]] = {
    commandsMap.get(KeyedCommand(treeNode.nodeType, name)) match {
      case Some(c) => Option(c)
      case _ => Option.empty
    }
  }
}

case class KeyedCommand(`type`: String, name: String)
