package com.atomist.tree.pathexpression

import com.atomist.rug.kind.DefaultTypeRegistry
import com.atomist.rug.kind.dynamic.ChildResolver
import com.atomist.rug.spi.{MutableView, TypeRegistry}
import com.atomist.tree.content.text._
import com.atomist.tree.pathexpression.ExecutionResult.ExecutionResult
import com.atomist.tree.{ContainerTreeNode, TreeNode}

/**
  * Inspired by XPath NodeTest. However, our NodeTests allow a change of
  * type.
  */
trait NodeTest {

  def follow(tn: TreeNode, axis: AxisSpecifier): ExecutionResult

}

/**
  * Convenience superclass that uses a predicate to handle sourced nodes
  *
  * @param predicate predicate used to filter nodes
  */
abstract class PredicatedNodeTest(name: String, predicate: Predicate) extends NodeTest {

  final override def follow(tn: TreeNode, axis: AxisSpecifier): ExecutionResult = sourceNodes(tn, axis) match {
    case Right(nodes) =>
      Right(nodes.filter(tn => predicate(tn, nodes)))
    case failure => failure
  }

  /**
    * Subclasses can override this to provide a more efficient implementation.
    * This one works but can be expensive.
    */
  protected def sourceNodes(tn: TreeNode, axis: AxisSpecifier): ExecutionResult = axis match {
    case Self => Right(List(tn))
    case Child => tn match {
      case ctn: ContainerTreeNode =>
        val kids = ctn.childNodes.toList
        Right(kids)
      case x => ExecutionResult.empty
    }
    case Descendant =>
      val kids = Descendant.allDescendants(tn).toList
      Right(kids)
    case DescendantOrSelf =>
      sourceNodes(tn, Descendant) match {
        case Right(nodes) => Right(tn +: nodes)
        case failure => failure
      }
  }
}

object All extends PredicatedNodeTest("All", TruePredicate)

case class NamedNodeTest(name: String)
  extends NodeTest {

  override def follow(tn: TreeNode, axis: AxisSpecifier): ExecutionResult = axis match {
    case Child =>
      tn match {
        case ctn: ContainerTreeNode =>
          val kids: List[TreeNode] =
            ctn.apply(name).toList match {
              case Nil =>
                TreeNodeOperations.invokeMethodIfPresent[TreeNode](tn, name).
                  map {
                    case s: List[TreeNode @unchecked] =>
                      s
                    case t: TreeNode =>
                      List(t)
                    case x =>
                      Nil
                  }.getOrElse {
                  val allKids = ctn.childNodes
                  val filteredKids = allKids.filter(n => name.equals(n.nodeName))
                  filteredKids.toList
                }
              case l => l
            }
          Right(kids)
        case x => Left(s"Cannot find property [$name] on non-container tree node [$x]")
      }
  }

}

/**
  *
  * @param typeName
  * @param nameWildcard * or a node name
  */
case class OfType(typeName: String, nameWildcard: String)
  extends PredicatedNodeTest(s"type=[$typeName]",
    Predicate(
      s"type=[$typeName],name=[$nameWildcard]",
      (tn, _) => tn.nodeType.equals(typeName) && (nameWildcard match {
        case "*" => true
        case name => tn.nodeName.equals(name)
      })
    )
  )

/**
  * We are going beyond obvious children. No equivalent in XPath.
  * For example: going to a type under children
  *
  * @param typeName name of the type
  */
case class TypeJump(typeName: String) extends NodeTest {

  private val typeRegistry: TypeRegistry = DefaultTypeRegistry

  private val childResolver: ChildResolver = typeRegistry.findByName(typeName) match {
    case Some(cr: ChildResolver) => cr
    case Some(x) => throw new IllegalArgumentException(s"Type [$x] does not support contextless resolution")
    case None => throw new IllegalArgumentException(s"No type with name [$typeName]")
  }

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
          val kids: Seq[TreeNode] = childResolver.findAllIn(mv).getOrElse(Nil)
          ExecutionResult(kids)
        case x => Left(s"Cannot find nodes of type name [$typeName] on non-view tree node [$x]")
      }
    case DescendantOrSelf =>
      tn match {
        case mv: MutableView[_] =>
          val kids: Seq[TreeNode] = childResolver.findAllIn(mv).getOrElse(Nil)
          ExecutionResult(kids)
        case x => Left(s"Cannot find nodes of type name [$typeName] on non-view tree node [$x]")
      }
  }
}