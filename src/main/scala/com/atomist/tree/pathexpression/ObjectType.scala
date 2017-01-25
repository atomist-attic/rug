package com.atomist.tree.pathexpression

import com.atomist.rug.kind.dynamic.ChildResolver
import com.atomist.rug.spi.{MutableView, TypeRegistry}
import com.atomist.tree.TreeNode
import com.atomist.tree.pathexpression.ExecutionResult.ExecutionResult
import com.atomist.util.misc.SerializationFriendlyLazyLogging

/**
  * Return all nodes of the given type
  *
  * @param typeName name of the type we're looking into
  */
case class ObjectType(typeName: String)
  extends NodeTest
    with SerializationFriendlyLazyLogging {

  private def childResolver(typeRegistry: TypeRegistry): Option[ChildResolver] =
    typeRegistry.findByName(typeName) match {
      case Some(cr: ChildResolver) => Some(cr)
      case _ => None
    }

  private val eligibleNode: TreeNode => Boolean = n => n.nodeType.contains(typeName)

  // Attempt to find nodes of the require type under the given node
  private def findMeUnder(tn: TreeNode, typeRegistry: TypeRegistry): Seq[TreeNode] =
    tn.childNodes.filter(eligibleNode) match {
      case Nil =>
        childResolver(typeRegistry).flatMap(cr => cr.findAllIn(tn)).getOrElse(Nil)
      case kids => kids
    }

  override def follow(tn: TreeNode, axis: AxisSpecifier, ee: ExpressionEngine, typeRegistry: TypeRegistry): ExecutionResult = {
    axis match {
      case NavigationAxis(propertyName) =>
        val nodes = tn.childrenNamed(propertyName).filter(eligibleNode)
        ExecutionResult(nodes)
      case Self => ExecutionResult(List(tn).filter(eligibleNode))
      case Child =>
        ExecutionResult(findMeUnder(tn, typeRegistry))
      case Descendant =>
        val allDescendants = Descendant.allDescendants(tn)
        val found = allDescendants.filter(eligibleNode) ++ allDescendants.flatMap(d => findMeUnder(d, typeRegistry))

        // We may have duplicates in the found collection because, for example,
        // we might find the Java() node SomeClass.java under the directory "/src"
        // and under "/src/main" and under the actual file.
        // So check that our returned types have distinct backing objects
        // TODO what happens if there isn't a mutable view with a concept of a backing object?
        var backingObjectsSeen: Set[Any] = Set()
        var toReturn = List.empty[TreeNode]
        found.foreach {
          case mv: MutableView[_] =>
            if (!backingObjectsSeen.contains(mv.originalBackingObject)) {
              backingObjectsSeen = backingObjectsSeen + mv.originalBackingObject
              toReturn = toReturn :+ mv
            }
          case n =>
            // There's no concept of a backing object here, so we have to take on trust that
            // we didn't find this thing > once
            toReturn = toReturn :+ n
        }
        ExecutionResult(toReturn)
    }
  }

}
