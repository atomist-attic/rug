package com.atomist.rug.runtime

import com.atomist.model.content.text.{PathExpressionEngine, TreeNode}
import com.atomist.param.ParameterValue
import com.atomist.rug.RugRuntimeException
import com.atomist.rug.kind.DefaultTypeRegistry
import com.atomist.rug.kind.dynamic.ContextlessViewFinder
import com.atomist.rug.spi._
import jdk.nashorn.api.scripting.{AbstractJSObject, JSObject, ScriptObjectMirror}

import scala.collection.JavaConverters._

case class Match(root: TreeNode, matches: _root_.java.util.List[TreeNode]) {
}

/**
  * JavaScript-friendly facade to PathExpressionEngine.
  * Paralleled by a UserModel TypeScript interface.
  */
class PathExpressionExposer {

  val typeRegistry: TypeRegistry = DefaultTypeRegistry

  val pee = new PathExpressionEngine

  def evaluate(tn: TreeNode, pe: Object): Match = {
    pe match {
      case som: ScriptObjectMirror =>
        val expr: String = som.get("expression").asInstanceOf[String]
        pee.evaluate(tn, expr) match {
          case Right(nodes) =>
            val m = Match(tn, nodes.asJava)
            m
        }
    }
  }

  /**
    * Return a single match. Throw an exception otherwise.
    */
  def scalar(root: TreeNode, expr: String): TreeNode = ???

  // cast the current node
  def as(root: TreeNode, name: String): TreeNode = ???

  // Find the children of the current node of this time
  def children(root: TreeNode, name: String) = {
    val typ = typeRegistry.findByName(name).getOrElse(???)
    typ match {
      case cvf: ContextlessViewFinder =>
        val kids = cvf.findAllIn(root.asInstanceOf[MutableView[_]]).getOrElse(Nil)
        kids.map(k => new SafeCommittingProxy(typ, k)).asJava
    }
  }

//  private def safeCommittingProxy(typ: Type, n: TreeNode): Object = {
//
//  }

}

private object MagicJavaScriptMethods {

  /**
    * Set of JavaScript magic methods that we should let Nashorn superclass handle
    * @return
    */
  def MagicMethods = Set("valueOf", "toString")
}

/**
  * Proxy that sits in front of tree nodes (including MutableView objects)
  * that (a) checks whether an invoked method is exposed on the relevant Type
  * object and (b) calls the commit() method of the object on all invocations of a
  * method that isn't read-only
  *
  * @param n
  */
class SafeCommittingProxy(typ: Type, n: TreeNode) extends AbstractJSObject {

  override def getMember(name: String): AnyRef = typ.typeInformation match {
    case x if MagicJavaScriptMethods.MagicMethods.contains(name) =>
      super.getMember(name)

    case st: StaticTypeInformation =>
      val possibleOps = st.operations.filter(
        op => name.equals(op.name))
      // TODO separate error message if wrong number of arguments
      if (possibleOps.isEmpty)
        throw new RugRuntimeException(null,
          s"Attempt to invoke method [$name] on type [${typ.name}]: Not an exported method")

      // TODO issue with primitives when trying to print
      new AbstractJSObject() {

        override def isFunction: Boolean = true

        override def call(thiz: scala.Any, args: AnyRef*): AnyRef = {
          val op = possibleOps.find(
            op => op.parameters.size == args.size)
          // TODO separate error message if wrong number of arguments
          if (op.isEmpty)
            throw new RugRuntimeException(null,
              s"Attempt to invoke method [$name] on type [${typ.name}] with ${args.size} arguments: No matching signature")
          val returned = op.get.invoke(n, args.toSeq)
          n match {
            case c : { def commit(): Unit } => c.commit()
            case _ =>
          }
          returned
        }
      }

    case _ =>
      // No static type information
      throw new IllegalStateException(s"No static type information is available for type [${typ.name}]: Probably an internal error")
  }
}

trait UserModelContext {

  def registry: Map[String, Object]

}

object DefaultUserModelContext extends UserModelContext {

  override val registry = Map(
    "PathExpressionEngine" -> new PathExpressionExposer
  )
}
