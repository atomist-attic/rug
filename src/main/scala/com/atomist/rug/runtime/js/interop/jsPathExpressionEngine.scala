package com.atomist.rug.runtime.js.interop

import java.util
import java.util.Collections

import com.atomist.graph.GraphNode
import com.atomist.rug.RugRuntimeException
import com.atomist.rug.kind.dynamic.ChildResolver
import com.atomist.rug.runtime.js.nashorn.jsSafeCommittingProxy
import com.atomist.rug.runtime.js.{JavaScriptObject, RugContext, SimpleExecutionContext}
import com.atomist.rug.spi._
import com.atomist.tree.pathexpression.{ExpressionEngine, PathExpression, PathExpressionEngine, PathExpressionParser}

import scala.collection.JavaConverters._

/**
  * Represents a Match from executing a PathExpression, exposed
  * to JavaScript/TypeScript behind a jsScalaHidingProxy
  *
  * @param root    root we evaluated path from
  * @param matches matches
  */
case class Match(root: GraphNode,
                   matches: _root_.java.util.List[GraphNode])

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
                              rugContext: RugContext,
                              typeRegistry: TypeRegistry,
                              val ee: ExpressionEngine = new PathExpressionEngine) {

  import jsSafeCommittingProxy._

  def this(rugContext: RugContext) {
    this(rugContext, rugContext.typeRegistry, new PathExpressionEngine)
  }

  /**
    * Return a customized version of this path expression engine for use in a specific
    * context, with its own microgrammar types.
    *
    * @param dynamicType JavaScript rest dynamic type definitions.
    * @return customized instance of this engine
    */
  def addType(dynamicType: Object): jsPathExpressionEngine = {
    val tr = new UsageSpecificTypeRegistry(typeRegistry,
      Seq(dynamicType).map(dynamicTypeDefinitionToTypeProvider)
    )
    new jsPathExpressionEngine(rugContext, tr, this.ee)
  }

  private def dynamicTypeDefinitionToTypeProvider(o: Object): Typed = o match {
    case som: JavaScriptObject if som.hasMember("typeName") =>
      // It's a type provider coded in JavaScript
      val tp = new JavaScriptBackedTypeProvider(som)
      tp
    case som: JavaScriptObject =>
      throw new RugRuntimeException(null, s"Unrecognized type. It has properties ${som.entries().keys.mkString(",")}")
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
  def evaluate(root: AnyRef, pe: Object): AnyRef = {
    evaluateInternal(root, jsPathExpressionEngine.pathExpressionFromObject(pe))
  }

  private def evaluateInternal(root: AnyRef, pe: Object): Match =
    evaluateParsed(root, jsPathExpressionEngine.pathExpressionFromObject(pe))

  private def evaluateParsed(rootAsObject: AnyRef, parsed: PathExpression) = {
    val root = toUnderlyingGraphNode(rootAsObject)
    val hydrated = rugContext.treeMaterializer.hydrate(rugContext.teamId, toUnderlyingGraphNode(root), parsed)
    ee.evaluate(hydrated, parsed,
      SimpleExecutionContext(typeRegistry, rugContext.repoResolver)
    ) match {
      case Right(nodes) =>
        val m = Match(root, wrap(nodes, typeRegistry))
        m
      case Left(_) =>
        Match(root, Collections.emptyList())
    }
  }

  private def toUnderlyingGraphNode(o: AnyRef): GraphNode = o match {
    case scp: jsSafeCommittingProxy =>
      // Unwrap this
      scp.node
    //case shp: jsScalaHidingProxy if shp.target.isInstanceOf[GraphNode] => shp.target.asInstanceOf[GraphNode]
    case tn: GraphNode => tn
    case som: JavaScriptObject =>
      JavaScriptBackedGraphNode.toGraphNode(som).getOrElse(
        throw new IllegalArgumentException(s"Can't convert script object $som to a GraphNode")
      )
    case x => throw new IllegalArgumentException(s"Can't convert $x to a GraphNode")
  }

  /**
    * Evaluate the path expression, applying a function
    * to each result.
    *
    * @param root  node to evaluate path expression against
    * @param pexpr path expression (compiled or string)
    * @param f     function to apply to each path expression
    */
  def `with`(root: AnyRef, pexpr: Object, f: Object): Unit = f match {
    case som: JavaScriptObject =>
      val r = evaluateInternal(root, pexpr)
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
    * @param root root of Tree. SafeCommittingProxy wrapping a TreeNode
    * @param pe   path expression of object
    */
  def scalar(root: AnyRef, pe: Object): GraphNode = {
    val res = evaluateInternal(root, pe)
    val ms = res.matches
    ms.size() match {
      // TODO use more specific exception type
      case 0 => throw new Exception(s"No matches found for path expression [${jsPathExpressionEngine.pathExpressionFromObject(pe)}]")
      case 1 => ms.get(0)
      case more => throw new PathExpressionException(root, pe, s"Too many matches found for path expression [${jsPathExpressionEngine.pathExpressionFromObject(pe)}! Found $more")
    }
  }

  private def matchReport(root: AnyRef, pe: Object) = {
    val pathExpression = jsPathExpressionEngine.pathExpressionFromObject(pe)

    def inner(report: Seq[String], lastEmptySteps: Option[PathExpression], steps: PathExpression): Seq[String] = {
      if (steps.locationSteps.isEmpty) {
        report // nowhere else to go
      } else evaluateParsed(root, steps).matches match {
        case empty if empty.isEmpty => // nothing found, keep looking
          inner(report, Some(steps), steps.dropLastStep)
        case nonEmpty => // something was found
          val dirtyDeets = if (nonEmpty.size > 1) "" else s" = ${nonEmpty.get(0)}"
          val myReport = Seq(s"$steps found ${nonEmpty.size}$dirtyDeets")
          val recentEmptyReport = lastEmptySteps.map(" " + _ + " found 0").toSeq
          val reportSoFar = myReport ++ recentEmptyReport ++ report
          inner(reportSoFar.map(" " + _), None, steps.dropLastStep)
      }
    }

    inner(Seq(), None, pathExpression).mkString("\n")
  }

  /**
    * Try to cast the given node to the required type.
    */
  def as(root: AnyRef, name: String): GraphNode =
    scalar(root, s"->$name")

  /**
    * Find the children of the current node of the named type.
    *
    * @param parent parent node we want to look under
    * @param name   name of the children we want to look for
    */
  def children(parent: AnyRef, name: String): util.List[GraphNode] = {
    val rootTn = toUnderlyingGraphNode(parent)
    val typ = typeRegistry.findByName(name).getOrElse(
      throw new IllegalArgumentException(s"Unknown type")
    )
    (typ, rootTn) match {
      case (cr: ChildResolver, mv: MutableView[_]) =>
        val kids = cr.findAllIn(mv).getOrElse(Nil)
        wrap(kids, typeRegistry)
      case _ => ???
    }
  }
}

class PathExpressionException(msg: String) extends RuntimeException(msg) {

  def this(root: AnyRef, pe: Object, problem: String, details: String = "") = {
    this(PathExpressionException.formatMessage(root, pe, problem, details))
  }
}

object PathExpressionException {

  def formatMessage(root: AnyRef, pe: Object, problem: String, details: String): String = {
    val pePrint = jsPathExpressionEngine.pathExpressionFromObject(pe)
    val rootString = root.toString
    val detailString = if (details.isEmpty) "" else s"\n$details"
    s"$problem evaluating [$pePrint] against [$rootString]$detailString"
  }
}

object jsPathExpressionEngine {

  /**
    * Parse path expression from a JavaScript-backed object with an "expression" property or a string.
    */
  def pathExpressionFromObject(pe: Object): PathExpression = pe match {
    case som: JavaScriptObject =>
      // Examine a JavaScript object passed to us. It's probably a
      // TypeScript class with an "expression" property
      val expr = som.stringProperty("expression")
      PathExpressionParser.parsePathExpression(expr)
    case expr: String =>
      PathExpressionParser.parsePathExpression(expr)
    case pe: PathExpression =>
      pe
  }
}
