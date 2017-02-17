package com.atomist.rug.runtime.js.interop

import java.util
import java.util.Collections

import com.atomist.graph.GraphNode
import com.atomist.rug.RugRuntimeException
import com.atomist.rug.kind.DefaultTypeRegistry
import com.atomist.rug.kind.dynamic.ChildResolver
import com.atomist.rug.runtime.js.interop.NashornUtils._
import com.atomist.rug.spi._
import com.atomist.tree.TreeNode
import com.atomist.tree.content.text.microgrammar._
import com.atomist.tree.content.text.microgrammar.dsl.MatcherDefinitionParser
import com.atomist.tree.pathexpression.{ExpressionEngine, PathExpression, PathExpressionEngine, PathExpressionParser}
import jdk.nashorn.api.scripting.ScriptObjectMirror

import scala.collection.JavaConverters._

/**
  * Represents a Match from executing a PathExpression, exposed
  * to JavaScript/TypeScript.
  *
  * @param root    root we evaluated path from
  * @param matches matches
  */
case class jsMatch(root: TreeNode, matches: _root_.java.util.List[jsSafeCommittingProxy])

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
                              teamContext: TeamContext,
                              val ee: ExpressionEngine = new PathExpressionEngine,
                              typeRegistry: TypeRegistry = DefaultTypeRegistry) {

  import jsSafeCommittingProxy._

  /**
    * Return a customized version of this path expression engine for use in a specific
    * context, with its own microgrammar types
    *
    * @param dynamicType JavaScript rest dynamic type definitions.
    *                    Presently,
    * @return customized instance of this engine
    */
  def addType(dynamicType: Object): jsPathExpressionEngine = {
    val tr = new UsageSpecificTypeRegistry(this.typeRegistry,
      Seq(dynamicType).map(dynamicTypeDefinitionToTypeProvider)
    )
    new jsPathExpressionEngine(teamContext, this.ee, tr)
  }

  private def dynamicTypeDefinitionToTypeProvider(o: Object): Typed = o match {
    case som: ScriptObjectMirror if hasDefinedProperties(som, "name", "grammar", "submatchers") =>
      // It's a microgrammar
      val name = stringProperty(som, "name")
      val grammar = stringProperty(som, "grammar")
      val submatchers = toJavaMap(som.getMember("submatchers"))
      //println(s"Parsing $name=$grammar with ${matcherRegistry}")
      val mg = MatcherMicrogrammarConstruction.matcherMicrogrammar(name, grammar, submatchers)
      new MicrogrammarTypeProvider(mg)
    case som: ScriptObjectMirror if hasDefinedProperties(som, "typeName") =>
      // It's a type provider coded in JavaScript
      val tp = new JavaScriptBackedTypeProvider(som)
      tp
    case som: ScriptObjectMirror =>
      throw new RugRuntimeException(null, s"Unrecognized type. It has properties ${som.entrySet().asScala.map(_.getKey).mkString(",")}")
    case x =>
      throw new RugRuntimeException(null, s"Unrecognized dynamic type $x")

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
  def evaluate(root: TreeNode, pe: Object): jsMatch =
    evaluateParsed(root, jsPathExpressionEngine.pathExpressionFromObject(pe))

  private def evaluateParsed(root: TreeNode, parsed: PathExpression) = {
    val hydrated = teamContext.treeMaterializer.hydrate(teamContext.teamId, toUnderlyingTreeNode(root), parsed)
    ee.evaluate(hydrated, parsed, typeRegistry) match {
      case Right(nodes) =>
        val m = jsMatch(root, wrap(nodes))
        m
      case Left(_) =>
        jsMatch(root, Collections.emptyList())
    }
  }

  // If the node is a SafeCommittingProxy, find the underlying object
  private def toUnderlyingTreeNode(o: GraphNode): GraphNode = o match {
    case scp: jsSafeCommittingProxy => scp.node
    case tn: GraphNode => tn
  }

  /**
    * Evaluate the path expression, applying a function
    * to each result
    *
    * @param root  node to evaluate path expression against
    * @param pexpr path expression (compiled or string)
    * @param f     function to apply to each path expression
    */
  def `with`(root: TreeNode, pexpr: Object, f: Object): Unit = f match {
    case som: ScriptObjectMirror =>
      val r = evaluate(root, pexpr)
      r.matches.asScala.foreach(m => {
        val args = Seq(m)
        // println(s"I am calling ${som} with value ${m}")
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
  def scalar(root: TreeNode, pe: Object): jsSafeCommittingProxy = {
    val res = evaluate(root, pe)
    val ms = res.matches
    ms.size() match {
      // TODO use more specific exception type
      case 0 => throw new Exception(s"No matches found for path expression [${jsPathExpressionEngine.pathExpressionFromObject(pe)}]")
      case 1 => ms.get(0)
      case more => throw new PathExpressionException(root, pe, s"Too many matches found for path expression [${jsPathExpressionEngine.pathExpressionFromObject(pe)}! Found $more")
    }
  }

  private def matchReport(root: TreeNode, pe: Object) = {
    val pathExpression = jsPathExpressionEngine.pathExpressionFromObject(pe)

    def inner(report: Seq[String], lastEmptySteps: Option[PathExpression], steps: PathExpression): Seq[String] = {
      if (steps.locationSteps.isEmpty) {
        report // nowhere else to go
      }
      else evaluateParsed(root, steps).matches match {
        case empty if empty.isEmpty => // nothing found, keep looking
          inner(report, Some(steps), steps.dropLastStep)
        case nonEmpty => // something was found
          val dirtyDeets = if (nonEmpty.size > 1) "" else s" = ${nonEmpty.get(0)}"
          val myReport = Seq(s"${steps} found ${nonEmpty.size}$dirtyDeets")
          val recentEmptyReport = lastEmptySteps.map(" " + _ + " found 0").toSeq
          val reportSoFar = myReport ++ recentEmptyReport ++ report
          inner(reportSoFar.map(" " + _), None, steps.dropLastStep)
      }
    }

    inner(Seq(), None, pathExpression).mkString("\n")
  }

  /**
    * Convenience method to avoid overloading in TypeScript,
    * which can cause problems with inheritance
    */
  def scalarStr(root: TreeNode, pe: String): jsSafeCommittingProxy =
    scalar(root, pe)

  /**
    * Try to cast the given node to the required type.
    */
  def as(root: TreeNode, name: String): jsSafeCommittingProxy = scalar(root, s"->$name")

  /**
    * Find the children of the current node of the named type
    *
    * @param parent parent node we want to look under
    * @param name   name of the children we want to look for
    */
  def children(parent: TreeNode, name: String): util.List[jsSafeCommittingProxy] = {
    val rootTn = toUnderlyingTreeNode(parent)
    val typ = typeRegistry.findByName(name).getOrElse(
      throw new IllegalArgumentException(s"Unknown type")
    )
    (typ, rootTn) match {
      case (cr: ChildResolver, mv: MutableView[_]) =>
        val kids = cr.findAllIn(mv).getOrElse(Nil)
        wrap(kids)
      case _ => ???
    }
  }

}

class PathExpressionException(msg: String) extends RuntimeException(msg) {

  def this(root: TreeNode, pe: Object, problem: String, details: String = "") = {
    this(PathExpressionException.formatMessage(root, pe, problem, details))
  }
}

object PathExpressionException {

  def formatMessage(root: TreeNode, pe: Object, problem: String, details: String): String = {
    val pePrint = jsPathExpressionEngine.pathExpressionFromObject(pe)
    val rootString = root.toString
    val detailString = if (details.isEmpty) "" else s"\n$details"
    s"$problem evaluating [$pePrint] against [$rootString]$detailString"
  }
}

object jsPathExpressionEngine {

  val matcherParser = new MatcherDefinitionParser

  /**
    * Parse path expression from a JavaScript-backed object with an "expression" property or a string
    */
  def pathExpressionFromObject(pe: Object): PathExpression = pe match {
    case som: ScriptObjectMirror =>
      // Examine a JavaScript object passed to us. It's probably a
      // TypeScript class with an "expression" property
      val expr = NashornUtils.stringProperty(som, "expression")
      PathExpressionParser.parsePathExpression(expr)
    case expr: String =>
      PathExpressionParser.parsePathExpression(expr)
  }

}
