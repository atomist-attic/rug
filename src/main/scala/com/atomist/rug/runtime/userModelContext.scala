package com.atomist.rug.runtime

import com.atomist.model.content.text.{PathExpression, PathExpressionEngine, PathExpressionParser, TreeNode}
import com.atomist.param.ParameterValue
import com.atomist.rug.RugRuntimeException
import com.atomist.rug.kind.DefaultTypeRegistry
import com.atomist.rug.kind.dynamic.ContextlessViewFinder
import com.atomist.rug.spi._
import com.atomist.util.Visitor
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
      case s: String =>
        pee.evaluate(tn, s) match {
          case Right(nodes) =>
            val m = Match(tn, nodes.asJava)
            m
        }
    }
  }

  /**
    * Return a single match. Throw an exception otherwise.
    */
  def scalar(root: TreeNode, pe: Object): TreeNode = {
    val res = evaluate(root, pe)
    val ms = res.matches
    ms.size() match {
      case 0 => throw new Exception("No matches found!")
      case 1 =>
        print(s"The node type is ${ms.get(0).nodeType}")
        ms.get(0)
      case _ => throw new Exception("Too many matches found!")
    }
  }


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
    *
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
  * @param typ Rug type we are fronting
  * @param n   node we are fronting
  */
class SafeCommittingProxy(typ: Type, n: TreeNode)
  extends AbstractJSObject /* with TreeNode */ {

  //  override def nodeName: String = n.nodeName
  //
  //  override def nodeType: String = n.nodeType
  //
  //  override def value: String = n.value
  //
  //  override def accept(v: Visitor, depth: Int): Unit = n.accept(v, depth)

  override def getMember(name: String): AnyRef = typ.typeInformation match {
    case x if MagicJavaScriptMethods.MagicMethods.contains(name) =>
      super.getMember(name)

    case st: StaticTypeInformation =>
      //println(s"Calling $name on ${typ.name}")
      val possibleOps = st.operations.filter(
        op => name.equals(op.name))
      // TODO separate error message if wrong number of arguments
      if (possibleOps.isEmpty)
        throw new RugRuntimeException(null,
          s"Attempt to invoke method [$name] on type [${
            typ.name
          }]: Not an exported method")

      new AbstractJSObject() {

        override def isFunction: Boolean = true

        override def call(thiz: scala.Any, args: AnyRef*): AnyRef = {
          //println(s"in, call, op=$name")
          val op = possibleOps.find(
            op => op.parameters.size == args.size)
          // TODO separate error message if wrong number of arguments
          if (op.isEmpty)
            throw new RugRuntimeException(null,
              s"Attempt to invoke method [$name] on type [${
                typ.name
              }] with ${
                args.size
              } arguments: No matching signature")
          val returned = op.get.invoke(n, args.toSeq)
          if (!op.get.readOnly) n match {
            case c: {
              def commit(): Unit
            } => c.commit()
            case _ =>
          }
          returned
        }
      }

    case _ =>
      // No static type information
      throw new IllegalStateException(s"No static type information is available for type [${
        typ.name
      }]: Probably an internal error")
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
