package com.atomist.rug.kind.dynamic

import com.atomist.tree.content.text._
import com.atomist.rug.RugRuntimeException
import com.atomist.rug.spi._
import com.atomist.source.{FileArtifact, StringFileArtifact}
import com.atomist.tree.{ContainerTreeNode, MutableTreeNode, SimpleTerminalTreeNode}
import com.typesafe.scalalogging.LazyLogging

class MutableTreeNodeUpdater(soo: MutableContainerTreeNode)
  extends Updater[FileArtifact]
    with LazyLogging {

  override def update(v: MutableView[FileArtifact]): Unit = {
    val newContent = soo.value
    logger.debug(s"${v.currentBackingObject.path}: Content updated from [${v.currentBackingObject.content}] to [${soo.value}]")
    v.updateTo(StringFileArtifact.updated(v.currentBackingObject, newContent))
  }
}

class MutableContainerTypeProvider extends TypeProvider(classOf[MutableContainerMutableView]) {

  override def description: String = "Generic container"
}

/**
  * Fronts any view with hierarchy
  *
  * @param originalBackingObject
  * @param parent
  */
class MutableContainerMutableView(
                                   originalBackingObject: MutableContainerTreeNode,
                                   parent: MutableView[_])
  extends ContainerTreeNodeView[MutableContainerTreeNode](originalBackingObject, parent)
    with LazyLogging {

  override def tags: Set[String] = originalBackingObject.tags ++ Set("MutableContainer")

  override def dirty: Boolean = originalBackingObject.dirty

  @ExportFunction(readOnly = false, description = "Set the value of the given key")
  def set(@ExportFunctionParameterDescription(name = "key",
    description = "The match key whose content you want")
          key: String,
          @ExportFunctionParameterDescription(name = "value",
            description = "The new value")
          newValue: String): Unit = {
    originalBackingObject.childrenNamed(key).toList match {
      case (sm: MutableTreeNode) :: Nil =>
        logger.debug(s"Updating ${sm.value} to $newValue on ${this.currentBackingObject.nodeName}")
        sm.update(newValue)
        require(dirty)
      case Nil =>
        throw new RugRuntimeException(null,
          s"Cannot find backing key '$key' in [$originalBackingObject]")
      case x =>
        logger.debug(s"Cannot update backing key '$key' in [$originalBackingObject]")
    }
    updateTo(currentBackingObject)
    parent.commit()
  }

  @ExportFunction(readOnly = false, description = "Append")
  def append(toAppend: String): Unit = {
    originalBackingObject match {
      case msoo: SimpleMutableContainerTreeNode => msoo.appendField(SimpleTerminalTreeNode("appended", toAppend))
    }
    parent.commit()
  }

  @ExportFunction(readOnly = false, description = "Update the whole value")
  def update(newValue: String): Unit = {
    originalBackingObject match {
      case msoo: MutableTreeNode =>
        msoo.update(newValue)
        //println(s"Updated to $msoo")
      case other => throw new Exception(s"waaah I don't know what to do with a ${other}")
    }
    require(dirty)
    parent.commit()
  }

  override protected def viewFrom(o: ContainerTreeNode): ContainerTreeNodeView[_] = o match {
    case suov: MutableContainerTreeNode => new MutableContainerMutableView(suov, this)
    case _ => super.viewFrom(o)
  }
}
