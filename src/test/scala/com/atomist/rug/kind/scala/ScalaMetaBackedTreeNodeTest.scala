package com.atomist.rug.kind.scala

import com.atomist.rug.kind.DefaultTypeRegistry
import com.atomist.tree.TreeNode
import com.atomist.tree.content.text.PositionedTreeNode
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
    val tn = new ScalaMetaTreeBackedTreeNode(str.get)
  }

  it should "satisfy simple path expression" in {
    val source =
      """class Foo(bar: String, i: Int)
      """
    val str: Parsed[Source] = source.parse[Source]
    val tn = new ScalaMetaTreeBackedTreeNode(str.get)
    ee.evaluate(tn, "//termParam[/typeName[@value='String']]/termName", DefaultTypeRegistry) match {
      case Right(nodes) if nodes.nonEmpty =>
        assert(nodes.size === 1)
        assert(nodes.head.asInstanceOf[TreeNode].value === "bar")
      case wtf => fail(s"Unexpected: $wtf")
    }
  }

  it should "return position information" in {
    val source =
      """class Foo(bar: String, i: Int)
      """
    val str: Parsed[Source] = source.parse[Source]
    val tn = new ScalaMetaTreeBackedTreeNode(str.get)
    ee.evaluate(tn, "//termParam[/typeName[@value='String']]/termName", DefaultTypeRegistry) match {
      case Right(nodes) if nodes.nonEmpty =>
        assert(nodes.size === 1)
        val tn: PositionedTreeNode = nodes.head.asInstanceOf[PositionedTreeNode]
        assert(tn.value === "bar")
        assert(tn.startPosition.offset === source.indexOf("bar"))
        //nodes.head.
      case wtf => fail(s"Unexpected: $wtf")
    }
  }
}
