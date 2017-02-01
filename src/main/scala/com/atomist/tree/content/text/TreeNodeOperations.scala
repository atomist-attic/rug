package com.atomist.tree.content.text

import com.atomist.tree.TreeNode
import org.springframework.util.ReflectionUtils

/**
  * Operations on TreeNodes such as tree pruning.
  */
object TreeNodeOperations {

  def invokeMethod[T](n: TreeNode, methodName: String, args: Seq[String]): T = {
    val m = ReflectionUtils.getAllDeclaredMethods(n.getClass).find(m => m.getName.equals(methodName) && m.getParameterCount == args.size).getOrElse(
      throw new IllegalArgumentException(s"Cannot find method with name [$methodName] and ${args.size} parameters on ${n.getClass}")
    )
    // if (!m.getReturnType.equals(classOf[T]) ???
    m.invoke(n, args: _*).asInstanceOf[T]
  }

  def invokeMethodIfPresent[T](n: TreeNode, methodName: String, args: Seq[String] = Nil): Option[T] = {
    ReflectionUtils.getAllDeclaredMethods(n.getClass).find(m => m.getName.equals(methodName) && m.getParameterCount == args.size)
      .map(m => m.invoke(n, args: _*).asInstanceOf[T])
  }

}
