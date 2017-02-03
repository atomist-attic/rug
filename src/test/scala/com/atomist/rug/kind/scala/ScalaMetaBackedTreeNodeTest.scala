package com.atomist.rug.kind.scala

import com.atomist.rug.kind.DefaultTypeRegistry
import com.atomist.tree.MutableTreeNode
import com.atomist.tree.pathexpression.{ExpressionEngine, PathExpressionEngine}
import org.scalatest.{FlatSpec, Matchers}

import scala.meta._

class ScalaMetaBackedTreeNodeTest extends FlatSpec with Matchers {

  import com.atomist.tree.pathexpression.PathExpressionParser._

  val ee: ExpressionEngine = new PathExpressionEngine

  it should "parse simple class without error" in {
    val source =
      """class Foo(bar: String, i: Int)
      """

    val str: Parsed[Source] = source.parse[Source]
    val tn = new ScalaMetaTreeBackedMutableTreeNode(str.get)
    //tn.accept(ConsoleVisitor, 0)
  }

  it should "satisfy simple path expression" in {
    val source =
      """class Foo(bar: String, i: Int)
      """

    val str: Parsed[Source] = source.parse[Source]
    val tn = new ScalaMetaTreeBackedMutableTreeNode(str.get)
    ee.evaluate(tn, "//TermParam[/TypeName[@value='String']]/TermName", DefaultTypeRegistry) match {
      case Right(nodes) if nodes.nonEmpty =>
        nodes.size should be(1)
        nodes.head.value should be("bar")
      case wtf => fail(s"Unexpected: $wtf")
    }
  }

  it should "update a node expression" in {
    val source =
      """class Foo(bar: String, i: Int)
      """
    val str: Parsed[Source] = source.parse[Source]
    val tn = new ScalaMetaTreeBackedMutableTreeNode(str.get)

    ee.evaluate(tn, "//TermParam[/TypeName[@value='String']]/TermName", DefaultTypeRegistry) match {
      case Right(nodes) if nodes.nonEmpty =>
        nodes.size should be(1)
        val mut = nodes.head.asInstanceOf[MutableTreeNode]
        mut.update("Thing")
        mut.value should be("Thing")
        mut.dirty should be(true)
      case wtf => fail(s"Unexpected: $wtf")
    }
  }

  it should "update a node expression and see changes in root" in pendingUntilFixed {
    val source =
      """class Foo(bar: String, i: Int)
      """
    val str: Parsed[Source] = source.parse[Source]
    val tn = new ScalaMetaTreeBackedMutableTreeNode(str.get)

    ee.evaluate(tn, "//TermParam[/TypeName[@value='String']]/TermName", DefaultTypeRegistry) match {
      case Right(nodes) if nodes.nonEmpty =>
        nodes.size should be(1)
        val mut = nodes.head.asInstanceOf[MutableTreeNode]
        mut.update("Thing")
        mut.value should be("Thing")
        mut.dirty should be(true)
      case wtf => fail(s"Unexpected: $wtf")
    }
    tn.value should be (source.replace("String", "Thing"))
  }

}
