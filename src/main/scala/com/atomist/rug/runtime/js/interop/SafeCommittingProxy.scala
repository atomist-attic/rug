package com.atomist.rug.runtime.js.interop

import com.atomist.rug.RugRuntimeException
import com.atomist.rug.command.DefaultCommandRegistry
import com.atomist.rug.spi.{CommandRegistry, MutableView, StaticTypeInformation, Typed}
import com.atomist.tree.TreeNode
import com.atomist.util.lang.TypeScriptArray
import jdk.nashorn.api.scripting.AbstractJSObject

/**
  * Proxy fronting tree nodes (including MutableView objects) exposed to JavaScript
  * that (a) checks whether an invoked method is exposed on the relevant Type
  * object and vetoes invocation otherwise and (b) calls the commit() method of the node if found on all invocations of a
  * method that isn't read-only
  *
  * @param typ  Rug type we are fronting
  * @param node node we are fronting
  */
class SafeCommittingProxy(typ: Typed,
                          val node: TreeNode,
                          commandRegistry: CommandRegistry = DefaultCommandRegistry)
  extends AbstractJSObject {

  import SafeCommittingProxy.MagicJavaScriptMethods

  override def getMember(name: String): AnyRef = typ.typeInformation match {
    case _ if MagicJavaScriptMethods.contains(name) =>
      super.getMember(name)

    case st: StaticTypeInformation =>
      val possibleOps = st.operations.filter(
        op => name.equals(op.name))

      if (possibleOps.isEmpty && commandRegistry.findByNodeAndName(node, name).isEmpty) {
            throw new RugRuntimeException(null,
              s"Attempt to invoke method [$name] on type [${typ.name}]: No exported method with that name")
      }


      new AbstractJSObject() {

        override def isFunction: Boolean = true

        override def call(thiz: scala.Any, args: AnyRef*): AnyRef = {
          possibleOps.find(op => op.parameters.size == args.size) match {
            case None =>
              commandRegistry.findByNodeAndName(node, name) match {
                case Some(c) =>
                  c.invokeOn(node)
                case _ =>
                  throw new RugRuntimeException(null,
                    s"Attempt to invoke method [$name] on type [${typ.name}] with ${args.size} arguments: No matching signature")
              }
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
                  new TypeScriptArray(l)
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

private object SafeCommittingProxy {

  /**
    * Set of JavaScript magic methods that we should let Nashorn superclass handle.
    */
  def MagicJavaScriptMethods = Set("valueOf", "toString")
}