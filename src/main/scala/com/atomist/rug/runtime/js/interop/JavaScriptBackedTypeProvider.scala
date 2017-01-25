package com.atomist.rug.runtime.js.interop

import java.util.Objects

import com.atomist.rug.kind.core.FileType
import com.atomist.rug.kind.dynamic.{ChildResolver, MutableContainerMutableView}
import com.atomist.rug.spi.{TypeProvider, Typed}
import com.atomist.tree.TreeNode
import jdk.nashorn.api.scripting.ScriptObjectMirror

import scala.collection.JavaConverters._
import NashornUtils._

/**
  * Type provider backed by a JavaScript object
  */
class JavaScriptBackedTypeProvider(
                                    jsTypeProvider: ScriptObjectMirror)
  extends TypeProvider(classOf[MutableContainerMutableView])
    with ChildResolver {

  override val name: String = stringProperty(jsTypeProvider, "typeName")

  override def description: String = s"JavaScript-backed type [$name]"

  override def resolvesFromNodeTypes: Set[String] =
    Set(Typed.typeClassToTypeName(classOf[FileType]))

  override def findAllIn(context: TreeNode): Option[Seq[TreeNode]] = {
    val r = jsTypeProvider.callMember("find", context)
    val nodes: Seq[TreeNode] = toScalaSeq(r).map(e =>
      new ScriptObjectBackedTreeNode(e.asInstanceOf[ScriptObjectMirror])
    )
    Some(nodes)
  }
}


/**
  * TreeNode backed by a JavaScript object
  */
private class ScriptObjectBackedTreeNode(som: ScriptObjectMirror) extends TreeNode {

  override def nodeName: String = stringFunction(som, "nodeName")

  lazy val kids: Seq[TreeNode] =
    toScalaSeq(som.callMember("children")) map {
      case som: ScriptObjectMirror => new ScriptObjectBackedTreeNode(som)
    }

  override def nodeType: Set[String] =
    toScalaSeq(som.callMember("nodeType")).map(s => Objects.toString(s)).toSet

  override def value: String = stringFunction(som, "value")

  override def childNodeNames: Set[String] = kids.map(k => k.nodeName).toSet

  override def childNodeTypes: Set[String] = kids.flatMap(k => k.nodeType).toSet

  override def childrenNamed(key: String): Seq[TreeNode] =
    kids.filter(k => k.nodeName == key)

}