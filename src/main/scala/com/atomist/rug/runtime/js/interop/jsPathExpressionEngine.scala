package com.atomist.rug.runtime.js.interop

import java.util
import java.util.Collections

import com.atomist.rug.RugRuntimeException
import com.atomist.rug.kind.DefaultTypeRegistry
import com.atomist.rug.kind.dynamic.ContextlessViewFinder
import com.atomist.rug.spi._
import com.atomist.tree.TreeNode
import com.atomist.tree.content.text.microgrammar._
import com.atomist.tree.content.text.microgrammar.dsl.MatcherDefinitionParser
import com.atomist.tree.pathexpression.{ExpressionEngine, PathExpressionEngine, PathExpressionParser}
import com.atomist.util.lang.TypeScriptArray
import jdk.nashorn.api.scripting.{ScriptObjectMirror, ScriptUtils}

import scala.collection.JavaConverters._

/**
  * Represents a Match from executing a PathExpression, exposed
  * to JavaScript/TypeScript.
  * Matches are actually TreeNodes, but wrapped in SafeCommittingProxy,
  * hence the list of matches is a list of Object. We need to use a
  * Java, rather than Scala, collection, for interop.
  *
  * @param root    root we evaluated path from
  * @param matches matches
  */
case class Match(root: Object, matches: _root_.java.util.List[Object])

/**
  * JavaScript-friendly facade to an ExpressionEngine.
  * Paralleled by a user model TypeScript interface.
  * One is shared between all users, backed by a global TypeRegistry.
  * Users can call the customize() method to add further dynamic
  * type definitions, such as microgrammars, in a specific usage.
  *
  * Parameters are detyped for interop. Not intended for use directly by Scala or Java callers,
  * which should use PathExpressionEngine, hence the unusual naming convention.
  *
  * @see ExpressionEngine
  * @param ee underlying ExpressionEngine that does the actual work
  */
class jsPathExpressionEngine(
                              val ee: ExpressionEngine = new PathExpressionEngine,
                              typeRegistry: TypeRegistry = DefaultTypeRegistry,
                              private var matcherRegistry: MatcherRegistry = EmptyMatcherRegistry) {

  /**
    * Return a customized version of this path expression engine for use in a specific
    * context, with its own microgrammar types
    * @param dynamicType JavaScript rest dynamic type definitions.
    *           Presently,
    * @return customized instance of this engine
    */
  def addType(dynamicType: Object): jsPathExpressionEngine = {
    val tr = new UsageSpecificTypeRegistry(this.typeRegistry,
      Seq(dynamicType).map(dynamicTypeDefinitionToTypeProvider)
    )
    new jsPathExpressionEngine(this.ee, tr, matcherRegistry)
  }

  private def dynamicTypeDefinitionToTypeProvider(o: Object): Typed = o match {
    case som: ScriptObjectMirror =>
      // It's a microgrammar
      val name = NashornUtils.stringProperty(som, "name")
      val grammar = NashornUtils.stringProperty(som, "grammar")
      if (name == null || grammar == null)
        throw new RugRuntimeException(null, "A microgrammar must specify both name and grammar")
      //println(s"Parsing $name=$grammar with ${matcherRegistry}")
      val parsedMatcher = jsPathExpressionEngine.matcherParser.parseMatcher(name, grammar, matcherRegistry)
      println("Parsed matcher=" + parsedMatcher)
      matcherRegistry += parsedMatcher
      val mg = new MatcherMicrogrammar(parsedMatcher)
      new MicrogrammarTypeProvider(mg)
  }

  /**
    * Evaluate the given path expression.
    *
    * @param root root node to evaluate path expression against. It's a tree node but we may need to unwrap it
    *             from a SafeCommittingProxy that we've passed to the JavaScript layer
    * @param pe   path expression to evaluate, which may be either a string or a
    *             JavaScript object visible with an "expression" property to allow reuse.
    *             The latter allows us to define TypeScript classes.
    * @return a Match
    */
  def evaluate(root: Object, pe: Object): Match = {
    pe match {
      case som: ScriptObjectMirror =>
        // Examine a JavaScript object passed to us. It's probably a
        // TypeScript class with an "expression" property
        val expr = NashornUtils.stringProperty(som, "expression")
        val parsed = PathExpressionParser.parsePathExpression(expr)
        ee.evaluate(toTreeNode(root), parsed, typeRegistry) match {
          case Right(nodes) =>
            val m = Match(root, wrap(nodes))
            m
          case Left(_) =>
            Match(root, Collections.emptyList())
        }
      case expr: String =>
        val parsed = PathExpressionParser.parsePathExpression(expr)
        ee.evaluate(toTreeNode(root), parsed, typeRegistry) match {
          case Right(nodes) =>
            val m = Match(root, wrap(nodes))
            m
        }
    }
  }

  // If the node is a SafeCommittingProxy, find the underlying object
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
    * Return a single match to the given path expression. Throw an exception otherwise.
    *
    * @param root root of Tree. SafeComittingProxy wrapping a TreeNode
    * @param pe   path expression of object
    */
  def scalar(root: Object, pe: Object): Object = {
    val res = evaluate(root, pe)
    val ms = res.matches
    ms.size() match {
      // TODO use more specific exception type
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

  /**
    * Find the children of the current node of the named type
    *
    * @param parent parent node we want to look under
    * @param name name of the children we want to look for
    */
  def children(parent: Object, name: String): util.List[Object] = {
    val rootTn = toTreeNode(parent)
    val typ = typeRegistry.findByName(name).getOrElse(???)
    (typ, rootTn) match {
      case (cvf: ContextlessViewFinder, mv: MutableView[_]) =>
        val kids = cvf.findAllIn(mv).getOrElse(Nil)
        wrap(kids)
      case _ => ???
    }
  }

  /**
    * Wrap the given sequence of nodes so they can be accessed from
    * TypeScript. Intended for use from Scala, not TypeScript.
    *
    * @param nodes sequence to wrap
    * @return TypeScript and JavaScript-friendly list
    */
  def wrap(nodes: Seq[TreeNode]): java.util.List[Object] = {
    new TypeScriptArray(nodes.map(k => new SafeCommittingProxy({
      typeRegistry.findByName(k.nodeType).getOrElse(
        throw new UnsupportedOperationException(s"Cannot find type for node type [${k.nodeType}]")
      )
    },
      k).asInstanceOf[Object]).asJava)
  }
}

object jsPathExpressionEngine {

  val matcherParser = new MatcherDefinitionParser

}
