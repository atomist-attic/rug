package com.atomist.tree.content.text

import java.lang.reflect.Method

import com.atomist.tree.{ContainerTreeNode, TreeNode}
import org.springframework.util.ReflectionUtils

/**
  * Export methods via reflection
  */
trait ReflectiveExportContainerTreeNode extends ContainerTreeNode {

  private case class KidMethod(m: Method) {
    def name = m.getName

    def typeName = m.getReturnType.getSimpleName

    def invoke(): Seq[TreeNode] = m.invoke(ReflectiveExportContainerTreeNode.this) match {
      case null => Nil
      case tn: TreeNode => Seq(tn)
      case Seq(tn: TreeNode) => Seq(tn)
      case x => Nil
    }
  }

  private val kidMethods =
    ReflectionUtils.getAllDeclaredMethods(getClass)
      .filter(m => m.getParameterCount == 0)
      //      .filter(m =>
      //        m.getReturnType.isAssignableFrom(classOf[TreeNode]) ||
      //          m.getReturnType.isAssignableFrom(classOf[java.util.Collection[_]]))
      .filter(m => m.getAnnotations.exists(a => a.isInstanceOf[ChildNode]))
      .map(m => KidMethod(m))

  final override def childNodes: Seq[TreeNode] = {
    kidMethods.flatMap(k => k.invoke())
  }

  final override def apply(key: String): Seq[TreeNode] = {
    kidMethods.find(k => k.name.equals(key)).map(k => k.invoke()).getOrElse(Nil)
  }

  // Get the return type simple names
  final override def childNodeTypes: Set[String] =
    kidMethods.map(k => k.typeName).toSet
}