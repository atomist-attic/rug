package com.atomist.rug.runtime.js.interop

import com.atomist.rug.RugRuntimeException
import com.atomist.rug.command.DefaultCommandRegistry
import com.atomist.rug.spi._
import com.atomist.tree.{ContainerTreeNode, TreeNode}
import com.atomist.util.lang.JavaScriptArray
import jdk.nashorn.api.scripting.AbstractJSObject

/**
  * Proxy fronting tree nodes (including MutableView objects) exposed to JavaScript
  * that (a) checks whether an invoked method is exposed on the relevant Type
  * object and vetoes invocation otherwise and (b) calls the commit() method of the node if found on all invocations of a
  * method that isn't read-only
  *
  * @param types Rug types we are fronting. This is a union type.
  * @param node  node we are fronting
  */
class jsSafeCommittingProxy(types: Set[Typed],
                            val node: TreeNode,
                            commandRegistry: CommandRegistry)
  extends AbstractJSObject {

  def this(t: Typed, node: TreeNode, commandRegistry: CommandRegistry = DefaultCommandRegistry) =
    this(Set(t), node, commandRegistry)

  override def toString: String = s"SafeCommittingProxy around $node"

  private val typ = UnionType(types)

  import jsSafeCommittingProxy.MagicJavaScriptMethods

  override def getMember(name: String): AnyRef = typ.typeInformation match {
    case _ if MagicJavaScriptMethods.contains(name) =>
      super.getMember(name)

    case st: StaticTypeInformation =>
      val possibleOps = st.operations.filter(
        op => name.equals(op.name))

      if (possibleOps.isEmpty && commandRegistry.findByNodeAndName(node, name).isEmpty) {
        if (node.nodeTags.contains(TreeNode.Dynamic)) {
          // Navigation on a node
          new FunctionProxyToNodeNavigationMethods(name, node)
        }
        else node match {
          case sobtn: ScriptObjectBackedTreeNode =>
            sobtn.invoke(name)
          case _ => throw new RugRuntimeException(null,
            s"Attempt to invoke method [$name] on type [${typ.description}]: No exported method with that name: Found ${st.operations.map(_.name)}")
        }
      }
      else
        new FunctionProxyToReflectiveInvocationOnUnderlyingNode(name, possibleOps)

    case _ =>
      // No static type information
      throw new IllegalStateException(s"No static type information is available for type [${typ.description}]: Probably an internal error")
  }

  /**
    * Nashorn proxy for a method invocation that delegates to the
    * underlying node using reflecti
    */
  private class FunctionProxyToReflectiveInvocationOnUnderlyingNode(name: String, possibleOps: Seq[TypeOperation])
    extends AbstractJSObject {

    override def isFunction: Boolean = true

    override def call(thiz: scala.Any, args: AnyRef*): AnyRef = {
      possibleOps.find(op => op.parameters.size == args.size) match {
        case None =>
          commandRegistry.findByNodeAndName(node, name) match {
            case Some(c) =>
              c.invokeOn(node)
            case _ =>
              throw new RugRuntimeException(null,
                s"Attempt to invoke method [$name] on type [${typ.description}] with ${args.size} arguments: No matching signature")
          }
        case Some(op) =>
          // Reflective invocation
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
              new JavaScriptArray(l)
            case _ => returned
          }
      }
    }
  }

  /**
    * Nashorn proxy for a method invocation that use navigation methods on
    * TreeNode
    */
  private class FunctionProxyToNodeNavigationMethods(name: String, node: TreeNode)
    extends AbstractJSObject {

    override def isFunction: Boolean = true

    override def call(thiz: scala.Any, args: AnyRef*): AnyRef = {
      import scala.language.reflectiveCalls

      val r = node match {
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
            case head :: Nil => head
            case more => ???
          }
        case _ => node
      }
      r
    }
  }

}

private object jsSafeCommittingProxy {

  /**
    * Set of JavaScript magic methods that we should let Nashorn superclass handle.
    */
  def MagicJavaScriptMethods = Set("valueOf", "toString")

}

/**
  * Return a type that exposes all the operations on the given set of types
  *
  * @param types set of types to expose
  */
private case class UnionType(types: Set[Typed]) extends Typed {

  override val name = s"Union(${types.map(_.name)})"

  private val typesToUnion = Set(TypeOperation.TreeNodeType) ++ types

  override def description: String = s"Union-${typesToUnion.map(_.name).mkString(":")}"

  // TODO what about duplicate names?
  override val typeInformation: TypeInformation = {
    val allOps: Set[TypeOperation] = (typesToUnion.map(_.typeInformation) collect {
      case sti: StaticTypeInformation => sti.operations
    }).flatten
    new StaticTypeInformation {
      override def operations: Seq[TypeOperation] = allOps.toSeq
    }
  }
}
