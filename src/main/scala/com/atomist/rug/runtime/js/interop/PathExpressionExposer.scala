package com.atomist.rug.runtime.js.interop

import com.atomist.rug.RugRuntimeException
import com.atomist.rug.kind.DefaultTypeRegistry
import com.atomist.rug.kind.dynamic.ContextlessViewFinder
import com.atomist.rug.spi.{MutableView, StaticTypeInformation, TypeRegistry, Typed}
import com.atomist.tree.TreeNode
import com.atomist.tree.pathexpression.{ExpressionEngine, PathExpressionEngine}
import com.atomist.util.lang.TypescriptArray
import jdk.nashorn.api.scripting.{AbstractJSObject, ScriptObjectMirror}

import scala.collection.JavaConverters._

/**
  * Represents a Match from executing a PathExpression.
  * Matches are actually TreeNodes, but wrapped in SafeCommittingProxy
  *
  * @param root    root we evaluated path from
  * @param matches matches
  */
case class Match(root: Object, matches: _root_.java.util.List[Object])


/**
  * JavaScript-friendly facade to PathExpressionEngine.
  * Paralleled by a UserModel TypeScript interface.
  */
class PathExpressionExposer {

  val typeRegistry: TypeRegistry = DefaultTypeRegistry

  val ee: ExpressionEngine = new PathExpressionEngine

  /**
    * Evaluate the given path expression
    *
    * @param root root node to evaluate path expression against. It's a tree node but we may need to unwrap it
    * @param pe   path expression to evaluate
    * @return a Match
    */
  def evaluate(root: Object, pe: Object): Match = {
    pe match {
      case som: ScriptObjectMirror =>
        val expr: String = som.get("expression").asInstanceOf[String]
        ee.evaluate(toTreeNode(root), expr) match {
          case Right(nodes) =>
            val m = Match(root, wrap(nodes))
            m
        }
      case s: String =>
        ee.evaluate(toTreeNode(root), s) match {
          case Right(nodes) =>
            val m = Match(root, wrap(nodes))
            m
        }
    }
  }

  private def toTreeNode(o: Object): TreeNode = o match {
    case tn: TreeNode => tn
    case scp: SafeCommittingProxy => scp.node
  }

  /**
    * Evalute the path expression, applying a function
    * to each result
    *
    * @param root  node to evaluate path expression against
    * @param pexpr path expression (compiled or string)
    * @param f     function to apply to each path expression
    */
  def `with`(root: Object, pexpr: Object, f: Object): Unit = f match {
    case som: ScriptObjectMirror =>
      val r = evaluate(root, pexpr)
      r.matches.asScala.foreach(m => {
        val args = Seq(m)
        som.call("apply", args: _*)
      })
    case _ =>
      throw new RugRuntimeException(null, s"Invalid argument to 'with' method: $f is not a JavaScript function")
  }

  /**
    * Return a single match. Throw an exception otherwise.
    *
    * @param root root of Tree. SafeComittingProxy wrapping a TreeNode
    * @param pe   path expression of object
    */
  def scalar(root: Object, pe: Object): Object = {
    val res = evaluate(root, pe)
    val ms = res.matches
    ms.size() match {
      case 0 => throw new Exception("No matches found!")
      case 1 =>
        ms.get(0)
      case _ => throw new Exception("Too many matches found!")
    }
  }

  /**
    * Try to cast the given node to the required type.
    */
  def as(root: Object, name: String): Object = scalar(root, s"->$name")

  // Find the children of the current node of this type
  def children(root: Object, name: String) = {
    val rootTn = toTreeNode(root)
    val typ = typeRegistry.findByName(name).getOrElse(???)
    (typ,rootTn) match {
      case (cvf: ContextlessViewFinder, mv: MutableView[_]) =>
        val kids = cvf.findAllIn(mv).getOrElse(Nil)
        wrap(kids)
      case _ => ???
    }
  }

  private def wrap(nodes: Seq[TreeNode]): java.util.List[Object] = {
    nodes.map(k => new SafeCommittingProxy({
      typeRegistry.findByName(k.nodeType).getOrElse(
        throw new UnsupportedOperationException(s"Cannot find type for node type [${k.nodeType}]")
      )
    },
      k).asInstanceOf[Object]).asJava
  }
}

private object MagicJavaScriptMethods {

  /**
    * Set of JavaScript magic methods that we should let Nashorn superclass handle.
    */
  def MagicMethods = Set("valueOf", "toString")
}

/**
  * Proxy fronting tree nodes (including MutableView objects) exposed to JavaScript
  * that (a) checks whether an invoked method is exposed on the relevant Type
  * object and vetoes invocation otherwise and (b) calls the commit() method of the node if found on all invocations of a
  * method that isn't read-only
  *
  * @param typ  Rug type we are fronting
  * @param node node we are fronting
  */
class SafeCommittingProxy(typ: Typed, val node: TreeNode)
  extends AbstractJSObject {

  override def getMember(name: String): AnyRef = typ.typeInformation match {
    case x if MagicJavaScriptMethods.MagicMethods.contains(name) =>
      super.getMember(name)

    case st: StaticTypeInformation =>
      val possibleOps = st.operations.filter(
        op => name.equals(op.name))
      if (possibleOps.isEmpty)
        throw new RugRuntimeException(null,
          s"Attempt to invoke method [$name] on type [${typ.name}]: No exported method with that name")

      new AbstractJSObject() {

        override def isFunction: Boolean = true

        override def call(thiz: scala.Any, args: AnyRef*): AnyRef = {
          possibleOps.find(op => op.parameters.size == args.size) match {
            case None =>
              throw new RugRuntimeException(null,
                s"Attempt to invoke method [$name] on type [${typ.name}] with ${args.size} arguments: No matching signature")
            case Some(op) =>
              val returned = op.invoke(node, args.toSeq)
              node match {
                //case c: { def commit(): Unit } =>
                case c: MutableView[_] if !op.readOnly =>
                  c.commit()
                case _ =>
              }
              // The returned type needs to be wrapped if it's
              // a collection
              returned match {
                case l: java.util.List[_] =>
                  new TypescriptArray(l)
                case _ => returned
              }
          }
        }
      }

    case _ =>
      // No static type information
      throw new IllegalStateException(s"No static type information is available for type [${typ.name}]: Probably an internal error")
  }
}