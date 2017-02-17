package com.atomist.rug.runtime.js.interop

import com.atomist.graph.GraphNode
import com.atomist.rug.RugRuntimeException
import com.atomist.rug.kind.DefaultTypeRegistry
import com.atomist.rug.spi._
import com.atomist.tree._
import com.atomist.util.lang.JavaScriptArray
import jdk.nashorn.api.scripting.{AbstractJSObject, ScriptObjectMirror}

/**
  * Proxy fronting tree nodes (including MutableView objects) exposed to JavaScript
  * that (a) checks whether an invoked method is exposed on the relevant Type
  * object and vetoes invocation otherwise and (b) calls the commit() method of the node if found on all invocations of a
  * method that isn't read-only
  *
  * @param node node we are fronting
  */
class jsSafeCommittingProxy(
                             val node: TreeNode,
                             behaviourRegistry: TreeNodeBehaviourRegistry = DefaultTreeNodeBehaviourRegistry,
                             typeRegistry: TypeRegistry = DefaultTypeRegistry)
  extends AbstractJSObject
    with TreeNode {

  override def toString: String = s"SafeCommittingProxy around $node"

  private val nodeTypes: Set[Typed] =
    node.nodeTags.flatMap(t => typeRegistry.findByName(t))

  private val typ: Typed = UnionType(nodeTypes)

  private var additionalMembers: Map[String, Object] = Map()

  import jsSafeCommittingProxy.MagicJavaScriptMethods

  //-----------------------------------------------------
  // Delegate TreeNode methods to backing node

  override def nodeName: String = node.nodeName

  override def value: String = node match {
    case tn: TreeNode => tn.value
    case _ => ""
  }

  override def childNodeNames: Set[String] = node.relatedNodeNames

  override def childNodeTypes: Set[String] = node.relatedNodeTypes

  override def childrenNamed(key: String): Seq[TreeNode] =
    node.relatedNodesNamed(key).map(new jsSafeCommittingProxy(_, behaviourRegistry, typeRegistry))

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
      // println(s"Adding function member [$name]")
      additionalMembers = additionalMembers ++ Map(name -> som)
    case x =>
      // println(s"Adding non-function member [$name]")
      additionalMembers = additionalMembers ++ Map(name -> x)
  }

  override def getMember(name: String): AnyRef = {
    // println(s"getMember: [$name]")
    // First, look for an added member
    additionalMembers.get(name) match {
      case Some(som: ScriptObjectMirror) =>
        // println(s"Going with added value [$som] for [$name]")
        som
      case Some(x) =>
        // println(s"Going with added non-function value [$x] for [$name]")
        x
      case None if MagicJavaScriptMethods.contains(name) =>
        super.getMember(name)
      case None if name == "toString" => new AlwaysReturns(node.toString)
      case _ => invokeConsideringTypeInformation(name)
    }
  }

  private def invokeConsideringTypeInformation(name: String): AnyRef = {
    val st = typ
    val possibleOps = st.allOperations.filter(
      op => name.equals(op.name))
    if (possibleOps.isEmpty && behaviourRegistry.findByNodeAndName(node, name).isEmpty) {
      invokeGivenNoMatchingOperationInTypeInformation(name, st)
    }
    else
      new FunctionProxyToReflectiveInvocationOnUnderlyingJVMNode(name, possibleOps)
  }

  private def invokeGivenNoMatchingOperationInTypeInformation(name: String, st: Typed) = {
    if (node.nodeTags.contains(TreeNode.Dynamic)) name match {
      case navigation if navigation == "parent" || node.relatedNodeNames.contains(navigation) =>
        new FunctionProxyToNodeNavigationMethods(navigation, node)
      case _ =>
        throw new UnsupportedOperationException(s"Function [$name] not implemented on node with name [${
          node.nodeName
        }]")
    }
    else node match {
      case sobtn: ScriptObjectBackedTreeNode =>
        // This object is wholly defined in JavaScript
        sobtn.invoke(name)
      case _ => throw new RugRuntimeException(null,
        s"Attempt to invoke method [$name] on type [${typ.description}]: " +
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
          behaviourRegistry.findByNodeAndName(node, name) match {
            case Some(c) => c.invokeOn(node)
            case _ => throw new RugRuntimeException(null,
              s"Attempt to invoke method [$name] on type [${typ.description}] with ${args.size} arguments: No matching signature")
          }
        case Some(op) =>
          // Reflective invocation
          val returned = op.invoke(node, args.toSeq)
          node match {
            // case c: { def commit(): Unit } =>
            case c: MutableView[_] if !op.readOnly => c.commit()
            case _ =>
          }
          // The returned type needs to be wrapped if it's a collection
          returned match {
            case l: java.util.List[_] => new JavaScriptArray(l)
            case _ => returned
          }
      }
    }
  }

  private class AlwaysReturns(what: AnyRef) extends AbstractJSObject {

    override def isFunction: Boolean = true

    override def call(thiz: scala.Any, args: AnyRef*): AnyRef = what
  }

  /**
    * Nashorn proxy for a method invocation that use navigation methods on
    * TreeNode
    */
  private class FunctionProxyToNodeNavigationMethods(name: String, node: GraphNode)
    extends AbstractJSObject {

    override def isFunction: Boolean = true

    override def call(thiz: scala.Any, args: AnyRef*): AnyRef = {
      import scala.language.reflectiveCalls

      val r: GraphNode = node match {
        case ctn: ContainerTreeNode =>
          val nodesAccessedThroughThisFunctionCall: Seq[TreeNode] = name match {
            case "parent" =>
              // Not all nodes have a parent
              ctn match {
                case hap: ({def parent(): TreeNode})@unchecked => Seq(hap.parent())
                case _ => Seq(null)
              }
            case _ => ctn.childrenNamed(name)
          }
          nodesAccessedThroughThisFunctionCall.toList match {
            case Nil => throw new RugRuntimeException(name, s"No children or function found for property $name on $node")
            case null :: Nil => null
            case head :: Nil => head
            case more => more.head
            // throw new IllegalStateException(s"Illegal list content (${nodesAccessedThroughThisFunctionCall.size}): $nodesAccessedThroughThisFunctionCall")
          }
        case _ => node
      }
      // For terminal nodes we want the wrapped value
      r match {
        case ttn: TerminalTreeNode => ttn.value
        case _ => jsSafeCommittingProxy.wrapOne(r)
      }
    }
  }

}

object jsSafeCommittingProxy {

  import scala.collection.JavaConverters._

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
  def wrap(nodes: Seq[GraphNode], cr: TreeNodeBehaviourRegistry = DefaultTreeNodeBehaviourRegistry): java.util.List[jsSafeCommittingProxy] = {
    new JavaScriptArray(
      nodes.map(n => wrapOne(n, cr))
        .asJava)
  }

  def wrapOne(n: GraphNode, cr: TreeNodeBehaviourRegistry = DefaultTreeNodeBehaviourRegistry): jsSafeCommittingProxy =
    new jsSafeCommittingProxy(n, cr)
}

/**
  * Return a type that exposes all the operations on the given set of types.
  *
  * @param types set of types to expose
  */
private case class UnionType(types: Set[Typed]) extends Typed {

  override val name = s"Union(${types.map(_.name)})"

  private val typesToUnion = Set(TypeOperation.TreeNodeType) ++ types

  override def description: String = s"Union-${typesToUnion.map(_.name).mkString(":")}"

  // TODO what about duplicate names?
  override val allOperations: Seq[TypeOperation] =
    typesToUnion.flatMap(_.allOperations).toSeq

  override val operations: Seq[TypeOperation] =
    typesToUnion.flatMap(_.operations).toSeq
}
