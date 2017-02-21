package com.atomist.rug.runtime.js.interop

import java.util.Objects

import com.atomist.graph.GraphNode
import com.atomist.rug.kind.dynamic.{ChildResolver, MutableContainerMutableView}
import com.atomist.rug.runtime.js.interop.NashornUtils._
import com.atomist.rug.spi.TypeProvider
import com.atomist.tree.TreeNode
import jdk.nashorn.api.scripting.{AbstractJSObject, ScriptObjectMirror}

/**
  * Type provider backed by a JavaScript object
  */
class JavaScriptBackedTypeProvider(
                                    jsTypeProvider: ScriptObjectMirror)
  extends TypeProvider(classOf[MutableContainerMutableView])
    with ChildResolver {

  override val name: String = stringProperty(jsTypeProvider, "typeName")

  override def description: String = s"JavaScript-backed type [$name]"

  override def findAllIn(context: GraphNode): Option[Seq[TreeNode]] = {
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
class ScriptObjectBackedTreeNode(som: ScriptObjectMirror) extends TreeNode {

  override def nodeName: String = stringFunction(som, "nodeName")

  lazy val kids: Seq[TreeNode] =
    toScalaSeq(som.callMember("children")) map {
      case som: ScriptObjectMirror => new ScriptObjectBackedTreeNode(som)
    }

  override def nodeTags: Set[String] =
    toScalaSeq(som.callMember("nodeTags")).map(s => Objects.toString(s)).toSet

  override def value: String = stringFunction(som, "value")

  override def childNodeNames: Set[String] = kids.map(k => k.nodeName).toSet

  override def childNodeTypes: Set[String] = kids.flatMap(k => k.nodeTags).toSet

  override def childrenNamed(key: String): Seq[TreeNode] =
    kids.filter(k => k.nodeName == key)

  /**
    * Invoke an arbitrary function defined in JavaScript
    */
  def invoke(name: String): AbstractJSObject = new InvokingFunctionProxy(name)

  private class InvokingFunctionProxy(name: String)
    extends AbstractJSObject {

    override def isFunction: Boolean = true

    override def call(thiz: scala.Any, args: AnyRef*): AnyRef = {
      som.callMember(name)
    }
  }

}
