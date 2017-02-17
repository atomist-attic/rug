package com.atomist.tree.utils

import com.atomist.graph.GraphNode
import com.atomist.tree.TreeNode
import org.springframework.util.ReflectionUtils

import scala.reflect.ClassTag

object NodeUtils {

  def value(gn: GraphNode): String = gn match {
    case tn: TreeNode => tn.value
    case _ => ""
  }

  /**
    * Invoke the given method if it exists and returns the appropriate type
    */
  def invokeMethodIfPresent[T](n: GraphNode, methodName: String, args: Seq[String] = Nil)
                              (implicit tag: ClassTag[T]): Option[T] = {
    ReflectionUtils.getAllDeclaredMethods(n.getClass).find(
      m => m.getName == methodName && m.getParameterCount == args.size && {
        //println(s"tag=${tag.runtimeClass},rt=${m.getReturnType}")
        m.getReturnType.isPrimitive ||  // Let boxing do its magic
          tag.runtimeClass.isAssignableFrom(m.getReturnType)
      }
    )
      .map(m => m.invoke(n, args: _*).asInstanceOf[T])
  }
}
