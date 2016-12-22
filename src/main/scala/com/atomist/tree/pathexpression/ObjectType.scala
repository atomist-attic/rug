package com.atomist.tree.pathexpression

import com.atomist.rug.kind.dynamic.ChildResolver
import com.atomist.rug.spi.{MutableView, TypeRegistry}
import com.atomist.tree.pathexpression.ExecutionResult.ExecutionResult
import com.atomist.tree.{ContainerTreeNode, TreeNode}

/**
  * Return all nodes of the given type
  *
  * @param typeName
  */
case class ObjectType(typeName: String) extends NodeTest {

  private def _childResolver(typeRegistry: TypeRegistry): Option[ChildResolver] = typeRegistry.findByName(typeName) match {
    case Some(cr: ChildResolver) => Some(cr)
    case None => throw new IllegalArgumentException(s"No type with name [$typeName]")
    case _ =>
      // The parent node doesn't support contextless resolution
      None
  }

  private def childResolver(typeRegistry: TypeRegistry) = _childResolver(typeRegistry).getOrElse(
    throw new IllegalArgumentException(s"Type [$typeName] does not support contextless resolution")
  )

  override def follow(tn: TreeNode, axis: AxisSpecifier, typeRegistry: TypeRegistry): ExecutionResult = {
    axis match {
      case Self => ExecutionResult(List(tn))
      case Child => tn match {
        case ctn: ContainerTreeNode =>
          val kids = {
            val directKids = ctn.childNodes.filter(n => typeName.equals(n.nodeType))
            if (directKids.nonEmpty)
              directKids
            else
              tn match {
                case mv: MutableView[_] =>
                  childResolver(typeRegistry).findAllIn(mv).getOrElse(Nil)
                case x =>
                  throw new UnsupportedOperationException(s"Type ${x.getClass} not yet supported for resolution")
              }
          }
          println(s"Returning ${kids.size} ${kids.mkString("\n")} for type $typeName under $tn")
          ExecutionResult(kids.toList)
        case _ => ExecutionResult.empty
      }
      case Descendant =>
        val kids = Descendant.allDescendants(tn).toList
        ExecutionResult(kids)
    }
  }
}
