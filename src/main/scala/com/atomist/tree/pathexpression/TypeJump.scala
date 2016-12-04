package com.atomist.tree.pathexpression

import com.atomist.rug.kind.DefaultTypeRegistry
import com.atomist.rug.kind.dynamic.ChildResolver
import com.atomist.rug.spi.{MutableView, TypeRegistry}
import com.atomist.tree.TreeNode
import com.atomist.tree.pathexpression.ExecutionResult.ExecutionResult

/**
  * Look for nodes of the given type that can be resolved from the present node,
  * whether among its children, or using a ChildResolver. No equivalent in XPath.
  * For example: going to a type under children
  *
  * @param typeName name of the type
  */
case class TypeJump(typeName: String) extends NodeTest {

  private val typeRegistry: TypeRegistry = DefaultTypeRegistry

  private val _childResolver: Option[ChildResolver] = typeRegistry.findByName(typeName) match {
    case Some(cr: ChildResolver) => Some(cr)
    case None => throw new IllegalArgumentException(s"No type with name [$typeName]")
    case _ =>
      // Doesn't support contextless resolution
      None
  }

  private def childResolver = _childResolver.getOrElse(
    throw new IllegalArgumentException(s"Type [$typeName] does not support contextless resolution")
  )

  override def follow(tn: TreeNode, axis: AxisSpecifier): ExecutionResult = axis match {
    case Self =>
      tn match {
        case mv: MutableView[_] =>
          Right(List(mv))
        case x =>
          // val kids: Seq[TreeNode] = childResolver.findAllIn(x.p).getOrElse(Nil)
          // ExecutionResult(kids)
          ???
        case x => Left(s"Cannot find nodes of type name [$typeName] on non-view tree node [$x]")
      }
    case Child =>
      tn match {
        case mv: MutableView[_] =>
          val kids: Seq[TreeNode] =
            if (mv.childNodeTypes.contains(typeName)) {
              // TODO inefficient
              val k = mv.childNodes.filter(n => typeName.equals(n.nodeType))
              println(s"Looked for children of type [$typeName] on $mv, found $k")
              k
            }
            else {
              println(s"Using ChildResolver")
              childResolver.findAllIn(mv).getOrElse(Nil)
            }
          ExecutionResult(kids)
        case x => Left(s"Cannot find nodes of type name [$typeName] on non-view tree node [$x]")
      }
    case DescendantOrSelf =>
      tn match {
        case mv: MutableView[_] =>
          //val kids: Seq[TreeNode] = childResolver.findAllIn(mv).getOrElse(Nil)
          // TODO inefficient
          val kids = Descendant.allDescendants(tn).toList
          val hits = kids.filter(n => typeName.equals(n.nodeType))
          ExecutionResult(hits)
        case x => Left(s"Cannot find nodes of type name [$typeName] on non-view tree node [$x]")
      }
  }
}

