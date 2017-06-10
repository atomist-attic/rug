package com.atomist.rug.runtime.js.interop

import java.util.Objects

import com.atomist.graph.GraphNode
import com.atomist.rug.kind.dynamic.ChildResolver
import com.atomist.rug.runtime.js.JavaScriptObject
import com.atomist.rug.spi.{TypeOperation, Typed}
import com.atomist.tree.TreeNode

/**
  * Type provider backed by a JavaScript object
  */
class JavaScriptBackedTypeProvider(jsTypeProvider: JavaScriptObject)
  extends Typed
    with ChildResolver {

  override val name: String = jsTypeProvider.stringProperty("typeName")

  override def description: String = s"JavaScript-backed type [$name]"

  override def findAllIn(context: GraphNode): Option[Seq[TreeNode]] = {
    val nodes = jsTypeProvider.callMember("find", context) match {
      case o: JavaScriptObject => o.values().map(e =>
        new ScriptObjectBackedTreeNode(e.asInstanceOf[JavaScriptObject]))
      case _ => Nil
    }

    Some(nodes)
  }

  override def allOperations: Seq[TypeOperation] = Seq() // SOBTNs are handled specially in jsSafeCommittingProxy

  override def operations: Seq[TypeOperation] = Seq()
}

/**
  * TreeNode backed by a JavaScript object
  */
class ScriptObjectBackedTreeNode(som: JavaScriptObject) extends TreeNode {

  override def nodeName: String = som.stringFunction("nodeName")

  lazy val kids: Seq[TreeNode] =
    som.callMember("children") match {
      case o: JavaScriptObject => o.values().map {
        case o: JavaScriptObject => new ScriptObjectBackedTreeNode(o)
      }
      case _ => Nil
    }

  override def nodeTags: Set[String] =
    som.callMember("nodeTags") match {
      case o: JavaScriptObject => o.values().map(s  => Objects.toString(s)).toSet
      case _ => Set()
    }

  override def value: String = som.stringFunction("value")

  override def childNodeNames: Set[String] = kids.map(k => k.nodeName).toSet

  override def childNodeTypes: Set[String] = kids.flatMap(k => k.nodeTags).toSet

  override def childrenNamed(key: String): Seq[TreeNode] =
    kids.filter(k => k.nodeName == key)

  /**
    * Invoke an arbitrary function defined in JavaScript
    */
  def invoke(name: String): JavaScriptObject = new InvokingFunctionProxy(name)

  private class InvokingFunctionProxy(fname: String)
    extends JavaScriptObject {
    override def hasMember(name: String): Boolean = ???

    override def getMember(name: String): AnyRef = ???

    override def setMember(name: String, value: AnyRef): Unit = ???

    override def callMember(name: String, args: AnyRef*): AnyRef = ???

    override def call(thisArg: AnyRef, args: AnyRef*): AnyRef = {
      som.callMember(fname,args)
    }

    override def isEmpty: Boolean = ???

    override def values(): Seq[AnyRef] = ???

    override def isSeq: Boolean = ???

    override def isFunction: Boolean = true

    override def eval(js: String): AnyRef = ???

    override def entries(): Map[String, AnyRef] = ???

    override def getNativeObject: AnyRef = ???
  }

}
