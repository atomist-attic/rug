package com.atomist.tree.content.text

import com.atomist.tree.SimpleTerminalTreeNode
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

    assert(soo.value === line)
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
        assert(fi.start.offset === f1.startPosition.offset)
        assert(fi.start.lineNumberFrom1 === 1)
        assert(fi.end.offset === f1.endPosition.offset)
        assert(fi.end.lineNumberFrom1 === 1)
      case _ => ???
    }
  }

  it should "not find format info for node that isn't a descendant" in {
    val inputA = "foo"
    val inputB = "bar"
    val line = inputA

    val f1 = new MutableTerminalTreeNode("a", inputA, LineHoldingOffsetInputPosition(line, 0))
    val f2 = new MutableTerminalTreeNode("b", inputB, LineHoldingOffsetInputPosition(line, inputA.length))

    val soo = new SimpleMutableContainerTreeNode("x", Seq(f1), startOf(line), endOf(line))
    soo.pad(line)

    soo.formatInfo(f2) should be(empty)
  }

  it should "find format info from nested node in single line" in {
    val inputA = "foo"
    val inputB = "bar"
    val inputC = "Lisbon"
    val inputD = "Alentejo"
    val unmatchedContent = "this is incorrect"
    val bollocks2 = "(more bollocks)"
    val line = inputA + unmatchedContent + inputB + inputC + bollocks2 + inputD

    val f1 = new MutableTerminalTreeNode("a", inputA, LineHoldingOffsetInputPosition(line, 0))
    val f2 = new MutableTerminalTreeNode("b", inputB, LineHoldingOffsetInputPosition(line, inputA.length + unmatchedContent.length))

    val ff1 = new MutableTerminalTreeNode("c1", inputC, LineHoldingOffsetInputPosition(line, inputA.length + unmatchedContent.length + inputB.length))
    val ff2 = new MutableTerminalTreeNode("c2", inputD, LineHoldingOffsetInputPosition(line, inputA.length + unmatchedContent.length + inputB.length + inputC.length + bollocks2.length))

    val f3 = new SimpleMutableContainerTreeNode("c", Seq(ff1, ff2), ff1.startPosition, endOf(line))

    val soo = SimpleMutableContainerTreeNode.wholeInput("x", Seq(f1, f2, f3), line)

    soo.formatInfo(SimpleTerminalTreeNode("x", "y")) should be (empty)

    assert(soo.formatInfo(f1).get.start.offset === f1.startPosition.offset)
    assert(soo.formatInfo(f2).get.start.offset === f2.startPosition.offset)
    assert(soo.formatInfo(ff1).get.start.offset === ff1.startPosition.offset)
  }

  it should "find format info from nested node in multiple lines" in {
    val inputA = "foo"
    val inputB = "bar"
    val inputC = "Lisbon"
    val inputD = "Alentejo\n"
    val unmatchedContent = "this \n\tis incorrect\n\n"
    val bollocks2 = "(more bollocks)"
    val line = inputA + unmatchedContent + inputB + inputC + bollocks2 + inputD

    val f1 = new MutableTerminalTreeNode("a", inputA, LineHoldingOffsetInputPosition(line, 0))
    val f2 = new MutableTerminalTreeNode("b", inputB, LineHoldingOffsetInputPosition(line, inputA.length + unmatchedContent.length))

    val ff1 = new MutableTerminalTreeNode("c1", inputC, LineHoldingOffsetInputPosition(line, inputA.length + unmatchedContent.length + inputB.length))
    val ff2 = new MutableTerminalTreeNode("c2", inputD, LineHoldingOffsetInputPosition(line, inputA.length + unmatchedContent.length + inputB.length + inputC.length + bollocks2.length))

    val f3 = new SimpleMutableContainerTreeNode("c", Seq(ff1, ff2), ff1.startPosition, endOf(line))

    val soo = SimpleMutableContainerTreeNode.wholeInput("x", Seq(f1, f2, f3), line)

    soo.formatInfo(SimpleTerminalTreeNode("x", "y")) should be (empty)

    assert(soo.formatInfo(f1).get.start.offset === f1.startPosition.offset)
    assert(soo.formatInfo(f2).get.start.offset === f2.startPosition.offset)
    assert(soo.formatInfo(ff1).get.start.offset === ff1.startPosition.offset)
    soo.formatInfo(ff1).get.start.lineNumberFrom1 should be > (1)
  }

}
