package com.atomist.tree.content.text

import com.atomist.tree.content.text.OffsetInputPosition.{endOf, startOf}
import org.scalatest.{FlatSpec, Matchers}

class PositionedMutableContainerTreeNodeTest extends FlatSpec with Matchers {

  it should "return correct value after pad" in {
    val inputA = "foo"
    val inputB = "bar"
    val padding = "werowiueoriwuer"
    val line = inputA + padding + inputB

    val f1 = new MutableTerminalTreeNode("a", inputA, LineHoldingOffsetInputPosition(line, 0))
    val f2 = new MutableTerminalTreeNode("b", inputB, LineHoldingOffsetInputPosition(line, inputA.length + padding.length))

    val soo = new SimpleMutableContainerTreeNode("x", Seq(f1, f2), startOf(line), endOf(line))
    soo.pad(line)

    soo.value should equal (line)
  }

  it should "refuse to find format info for child node without pad" in {
    val inputA = "foo"
    val inputB = "bar"
    val line = inputA + inputB

    val f1 = new MutableTerminalTreeNode("a", inputA, LineHoldingOffsetInputPosition(line, 0))
    val f2 = new MutableTerminalTreeNode("b", inputB, LineHoldingOffsetInputPosition(line, inputA.length))

    val soo = new SimpleMutableContainerTreeNode("x", Seq(f1, f2), startOf(line), endOf(line))

    an[IllegalStateException] should be thrownBy soo.formatInfo(f1)
  }

  it should "find format info for child node" in {
    val inputA = "foo"
    val inputB = "bar"
    val line = inputA + inputB

    val f1 = new MutableTerminalTreeNode("a", inputA, LineHoldingOffsetInputPosition(line, 0))
    val f2 = new MutableTerminalTreeNode("b", inputB, LineHoldingOffsetInputPosition(line, inputA.length))

    val soo = new SimpleMutableContainerTreeNode("x", Seq(f1, f2), startOf(line), endOf(line))
    soo.pad(line)

    soo.formatInfo(f1) match {
      case Some(fi) =>
        fi.start.offset should be(f1.startPosition.offset)
        fi.start.lineNumberFrom1 should be(1)
        fi.end.offset should be(f1.endPosition.offset)
        fi.end.lineNumberFrom1 should be(1)
      case _ => ???
    }
  }

  it should "not find format info for non child node" in {
    val inputA = "foo"
    val inputB = "bar"
    val line = inputA

    val f1 = new MutableTerminalTreeNode("a", inputA, LineHoldingOffsetInputPosition(line, 0))
    val f2 = new MutableTerminalTreeNode("b", inputB, LineHoldingOffsetInputPosition(line, inputA.length))

    val soo = new SimpleMutableContainerTreeNode("x", Seq(f1), startOf(line), endOf(line))
    soo.pad(line)

    soo.formatInfo(f2) should be(empty)
  }

}
