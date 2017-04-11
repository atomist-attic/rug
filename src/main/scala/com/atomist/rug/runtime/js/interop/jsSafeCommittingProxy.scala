package com.atomist.rug.runtime.js.interop

import java.util

import com.atomist.graph.GraphNode
import com.atomist.rug.RugRuntimeException
import com.atomist.rug.spi._
import com.atomist.rug.ts.Cardinality
import com.atomist.tree._
import com.atomist.tree.utils.NodeUtils
import com.atomist.util.lang.JavaScriptArray
import com.atomist.util.misc.SerializationFriendlyLazyLogging
import jdk.nashorn.api.scripting.{AbstractJSObject, ScriptObjectMirror}

/**
  * Proxy fronting tree nodes (including MutableView objects) exposed to JavaScript
  * that (a) checks whether an invoked method is exposed on the relevant Type
  * object and vetoes invocation otherwise and (b) calls the commit() method of the node if found on all invocations of a
  * method that isn't read-only.
  *
  * @param node node we are fronting
  */
class jsSafeCommittingProxy(
                             val node: GraphNode,
                             typeRegistry: TypeRegistry)
  extends AbstractJSObject
    with TreeNode with SerializationFriendlyLazyLogging {

  import jsSafeCommittingProxy._

  override def toString: String = s"SafeCommittingProxy#$hashCode around $node"

  private val typ: Typed = Typed.typeFor(node, typeRegistry)

  // Members the user has added dynamically in JavaScript,
  // for example in mixins
  private var additionalMembers: Map[String, Object] = Map()

  //-----------------------------------------------------
  // Delegate GraphNode and TreeNode methods to backing node
  // We need to override all of them to wrap the results in ourselves

  override def nodeName: String = node.nodeName

  override def value: String = NodeUtils.value(node)

  override def childNodeNames: Set[String] = node.relatedNodeNames

  override def childNodeTypes: Set[String] = node.relatedNodeTypes

  override def childrenNamed(key: String): Seq[TreeNode] =
    node match {
      case tn: TreeNode =>
        tn.childrenNamed(key).map(wrapOne(_)).map(_.asInstanceOf[TreeNode])
      case unTreeNode =>
        throw new IllegalStateException(s"Attempt to invoke TreeNode navigation method on non TreeNode [$unTreeNode]")
    }

  override def relatedNodes: Seq[GraphNode] =
    node.relatedNodes.map(wrapOne)

  override def relatedNodesNamed(key: String): Seq[GraphNode] =
    node.relatedNodesNamed(key).map(wrapOne)

  private def wrapOne(n: GraphNode) = jsSafeCommittingProxy.wrapOne(n, typeRegistry)

  /**
    * A user is adding a named member e.g.
    * shouldTerm.dispatch_me = function(name) { ...
    * We want to save it and invoke it in getMember if necessary.
    *
    * @param name  name of the member
    * @param value function the user is adding in JavaScript
    */
  override def setMember(name: String, value: Object): Unit = value match {
    case som: ScriptObjectMirror =>
      logger.debug(s"JavaScript code has added function member [$name]")
      additionalMembers = additionalMembers ++ Map(name -> som)
    case x =>
      logger.debug(s"JavaScript code has added non-function member [$name]")
      additionalMembers = additionalMembers ++ Map(name -> x)
  }

  override def getMember(name: String): AnyRef = {
    // First, look for a member added in JavaScript
    val member = additionalMembers.get(name) match {
      case Some(som: ScriptObjectMirror) =>
        som
      case Some(x) =>
        x
      case None if MagicJavaScriptMethods.contains(name) =>
        super.getMember(name)
      case None if name == "toString" =>
        new AlwaysReturns(toString)
      case _ =>
        // Invoke using navigation on underlying node or reflection
        invokeConsideringTypeInformation(name)
    }
    logger.debug(s"Returning member [$member] for $name in $this")
    member
  }

  private def invokeConsideringTypeInformation(name: String): AnyRef = {
    val possibleOps = typ.allOperations.filter(op => name == op.name)
    if (possibleOps.nonEmpty) {
      val op = possibleOps.head //TODO seems fragile
      if (op.invocable) {
        val function = new FunctionProxyToReflectiveInvocationOnUnderlyingJVMNode(name, possibleOps)
        if (op.exposeAsProperty) {
          // Reuse the logic we have in the function implementation by simply creating and invoking it
          function.call("whatever")
        }
        else {
          function
        }
      }
      else
        nodeNavigationPropertyAccess(node, name)
    }
    else
      invokeGivenNoMatchingOperationInTypeInformation(name, typ)
  }

  private def invokeGivenNoMatchingOperationInTypeInformation(name: String, st: Typed): AnyRef = {
    if (node.nodeTags.contains(TreeNode.Dynamic)) name match {
      case navigation if node.relatedNodeNames.contains(navigation) =>
        nodeNavigationPropertyAccess(node, navigation)
      case _ =>
        throw new UnsupportedOperationException(
          s"""Function [$name] cannot be evaluated on node with name [${node.nodeName}]
             |Check that the function is defined on type [${node.nodeTags.headOption.getOrElse("???")}] and that the relationship is materialized
             |""".stripMargin +
            s"Type information=[$st]")
    }
    else node match {
      case sobtn: ScriptObjectBackedTreeNode =>
        // This object is wholly defined in JavaScript
        sobtn.invoke(name)
      case _ =>
        throw new RugRuntimeException(null,
          s"Attempt to invoke method [$name] on type [${typ.description}]: " +
            s"Wrapping node named ${node.nodeName}; " +
            s"No exported method with that name: Found ${st.allOperations.map(_.name).sorted}. Node tags are ${node.nodeTags}")
    }
  }

  /**
    * Nashorn proxy for a method invocation that delegates to the
    * underlying node using reflection.
    */
  private class FunctionProxyToReflectiveInvocationOnUnderlyingJVMNode(name: String, possibleOps: Seq[TypeOperation])
    extends AbstractJSObject {

    override def isFunction: Boolean = true

    override def call(thiz: scala.Any, args: AnyRef*): AnyRef = {
      possibleOps.find(op => op.parameters.size == args.size) match {
        case None =>
          throw new RugRuntimeException(null,
            s"Attempt to invoke method [$name] on type [${typ.description}] with ${args.size} arguments: No matching signature")
        case Some(op) =>
          // Reflective invocation
          val returned = op.invoke(node, args.toSeq)
          node match {
            case c: MutableView[_] if !op.readOnly => c.commit()
            case _ =>
          }
          // The returned type needs to be wrapped if it's a collection
          wrapJVMReturn(returned, op)
      }
    }

    import scala.collection.JavaConverters._

    private def wrapJVMReturn(returned: AnyRef, op: TypeOperation): AnyRef = {
      returned match {
        case l: util.List[_] => new JavaScriptArray(wrapIfNecessary(l.asScala, typeRegistry))
        case s: Seq[_] => new JavaScriptArray(wrapIfNecessary(s, typeRegistry))
        case s: Set[_] => new JavaScriptArray(s.toSeq.asJava)
        case r =>
          // Be sure to proxy all the way down
          val wrapped = wrapIfNecessary(Seq(r), typeRegistry).get(0)
          if (op.exposeResultDirectlyToNashorn)
            jsScalaHidingProxy(wrapped, returnNotToProxy = DoNotProxy)
          else
            wrapped
      }
    }
  }

  /**
    * Nashorn property access using navigation methods on TreeNode.
    */
  private def nodeNavigationPropertyAccess(node: GraphNode, name: String): AnyRef = {

    import scala.language.reflectiveCalls
    val nodesAccessedThroughThisFunctionCall: Seq[GraphNode] = node.relatedNodesNamed(name)
    nodesAccessedThroughThisFunctionCall.toList match {
      case Nil =>
        throw new RugRuntimeException(name,
          s"No children or function found for property '$name' on $node")
      case null :: Nil => null
      case (ttn: TerminalTreeNode) :: Nil if ttn.nodeTags.contains(Cardinality.One2Many) =>
        // Pull out the value and put in an array
        JavaScriptArray.fromSeq(Seq(ttn.value))
      case (ttn: TerminalTreeNode) :: Nil =>
        // Pull out the value
        ttn.value
      case head :: Nil if !head.nodeTags.contains(Cardinality.One2Many) =>
        // Only one entry and we know it's not marked as an array
        wrapOne(head)
      case more => new JavaScriptArray(wrapIfNecessary(more, typeRegistry))
    }
  }

}

object jsSafeCommittingProxy {

  import scala.collection.JavaConverters._

  /** To not proxy in scala hiding proxy */
  val DoNotProxy: Any => Boolean = r =>
    r.isInstanceOf[jsPathExpressionEngine] || r.isInstanceOf[GraphNode]

  /**
    * Set of JavaScript magic methods that we should let Nashorn superclass handle.
    */
  private[interop] def MagicJavaScriptMethods = Set("valueOf")

  /**
    * Wrap the given sequence of nodes so they can be accessed from
    * TypeScript. Intended for use from Scala, not TypeScript.
    *
    * @param nodes sequence to wrap
    * @return TypeScript and JavaScript-friendly list
    */
  def wrap(nodes: Seq[GraphNode], typeRegistry: TypeRegistry): java.util.List[GraphNode] =
    new JavaScriptArray(nodes.map(wrapOne(_, typeRegistry)).asJava)

  def wrapIfNecessary(nodes: Seq[_], typeRegistry: TypeRegistry): java.util.List[Object] = {
    val wrapped = nodes map {
      case n: GraphNode => wrapOne(n, typeRegistry)
      case x => x
    }
    wrapped.map(_.asInstanceOf[Object]).asJava
  }

  def wrapOne(n: GraphNode, typeRegistry: TypeRegistry): GraphNode = n match {
    case jsp: jsSafeCommittingProxy =>
      jsp
    case _ =>
      new jsSafeCommittingProxy(n, typeRegistry)
  }

}
