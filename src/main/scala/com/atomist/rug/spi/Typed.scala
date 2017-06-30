package com.atomist.rug.spi

import com.atomist.graph.GraphNode
import com.atomist.util.lang.JavaHelpers

object Typed {

  def typeFor(node: GraphNode, typeRegistry: TypeRegistry): Typed = {
    val nodeTypes: Set[Typed] =
      node.nodeTags.flatMap(t => typeRegistry.findByName(t))
    nodeTypes.size match {
      case 0 => TypeOperation.TreeNodeType
      case _ => UnionType(nodeTypes)
    }
  }

  private[spi] def trimSuffix(suffix: String, orig: String): String =
    if (orig == suffix)
      orig
    else
      orig stripSuffix suffix

  private val TypeSuffix = "Type"
  private val TreeNodeSuffix = "TreeNode"
  private val MutableViewSuffix = "MutableView"

  def typeClassToTypeName(tc: Class[_]): String = trimSuffix(TypeSuffix, tc.getSimpleName)

  def typeClassesToTypeNames(tcs: Class[_]*): Set[String] = tcs.map(typeClassToTypeName).toSet

  def typeToTypeName(tc: Class[_], searchable: Boolean = true): String = {
    val raw = trimSuffix(TreeNodeSuffix, trimSuffix(MutableViewSuffix, tc.getSimpleName))
    if (searchable) raw
    else JavaHelpers.lowerize(raw)
  }

}

/**
  * Extended by language elements to return as much type information as
  * possible to help with compile time validation and tooling.
  */
trait Typed {

  /**
    * Name for use in Rug scripts. e.g "file" in "with File f".
    *
    * @return alias for use in Rug scripts
    */
  val name: String = Typed.typeClassToTypeName(getClass)

  /**
    * Description of this type.
    */
  def description: String

  /**
    * Operations on the type including its inheritance hierarchy.
    */
  def allOperations: Seq[TypeOperation]

  /**
    * Operations on the type.
    */
  def operations: Seq[TypeOperation]
}

/**
  * Return a type that exposes all the operations on the given set of types.
  *
  * @param types set of types to expose
  */
private case class UnionType(types: Set[Typed]) extends Typed {

  private val typesToUnion = Set(TypeOperation.TreeNodeType) ++ types

  override val name: String = typesToUnion.map(_.name).mkString(" & ")

  override def description: String = name

  // TODO what about duplicate names?
  override lazy val allOperations: Seq[TypeOperation] =
    typesToUnion.flatMap(_.allOperations).toSeq

  override lazy val operations: Seq[TypeOperation] =
    typesToUnion.flatMap(_.operations).toSeq

  override def toString: String =
    s"$name: ops=${allOperations.sortBy(_.name).map(_.name).mkString(",")}"
}
