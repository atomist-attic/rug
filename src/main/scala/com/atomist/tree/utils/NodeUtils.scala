package com.atomist.tree.utils

import com.atomist.graph.GraphNode
import com.atomist.rug.kind.grammar.ParsedNode
import com.atomist.tree.TreeNode
import com.atomist.tree.content.text.PositionedTreeNode
import org.springframework.util.ReflectionUtils

import scala.reflect.ClassTag

/**
  * Convenience methods on GraphNode
  */
object NodeUtils {

  /**
    * Value of the related node if available, else None
    */
  def keyValue(gn: GraphNode, key: String): Option[String] = {
    gn.relatedNodesNamed(key).headOption match {
      case Some(tn: TreeNode) => Some(tn.value)
      case _ => None
    }
  }

  /**
    * Throw an exception if this key value cannot be extracted as a string
    * @param customMessage error message to use if desired
    */
  def requiredKeyValue(gn: GraphNode, key: String, customMessage: Option[String] = None): String = {
    requiredKey(gn, key, customMessage) match {
      case tn: TreeNode => tn.value
      case _ => throw new IllegalArgumentException(
        customMessage.getOrElse(s"No key named [$key] on $gn")
      )
    }
  }

  def requiredKey(gn: GraphNode, key: String, customMessage: Option[String] = None): GraphNode = {
    gn.relatedNodesNamed(key).headOption match {
      case Some(gn: GraphNode) => gn
      case _ =>
        gn.followEdge(key).toList match {
          case List(n: GraphNode) => n
          case l =>
            throw new IllegalArgumentException(
              customMessage.getOrElse(s"No single key value named [$key] on $gn: Found $l")
            )
        }
    }
  }

  def requiredNodeOfType(gn: GraphNode, tag: String, customMessage: Option[String] = None): GraphNode = {
    gn.relatedNodes.find(_.hasTag(tag)) match {
      case Some(gn: GraphNode) => gn
      case _ => throw new IllegalArgumentException(
        customMessage.getOrElse(s"No key with tag [$tag] on $gn"))
    }
  }

  def value(gn: GraphNode): String = gn match {
    case tn: TreeNode => tn.value
    case _ => ""
  }

  def positionedValue(gn: Object, in: String): String = gn match {
    case pn: ParsedNode => in.substring(pn.startOffset, pn.endOffset)
    case _ => ???
  }

  /**
    * Invoke the given method if it exists and returns the appropriate type
    */
  def invokeMethodIfPresent[T](n: GraphNode, methodName: String, args: Seq[String] = Nil)
                              (implicit tag: ClassTag[T]): Option[T] = {
    ReflectionUtils.getAllDeclaredMethods(n.getClass).find(
      m => m.getName == methodName && m.getParameterCount == args.size && {
        m.getReturnType.isPrimitive || // Let boxing do its magic
          tag.runtimeClass.isAssignableFrom(m.getReturnType)
      }
    )
      .map(m => m.invoke(n, args: _*).asInstanceOf[T])
  }

  def hasNoArgMethod[T](n: GraphNode, methodName: String)
                       (implicit tag: ClassTag[T]): Boolean = {
    ReflectionUtils.getAllDeclaredMethods(n.getClass).exists(
      m => m.getName == methodName && m.getParameterCount == 0 && {
        tag.runtimeClass.isAssignableFrom(m.getReturnType)
      })
  }
}
