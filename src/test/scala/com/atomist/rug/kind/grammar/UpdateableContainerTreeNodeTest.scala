package com.atomist.rug.kind.grammar

import com.atomist.model.content.text._
import org.scalatest.{FlatSpec, Matchers}

class UpdateableContainerTreeNodeTest extends FlatSpec with Matchers {

  import OffsetInputPosition._

  it should "update first scalar match within object value" in {
    val inputA = "foo"
    val inputB = "bar"
    val line = inputA + inputB

    val f1 = new MutableTerminalTreeNode("a", inputA, LineHoldingOffsetInputPosition(line, 0))
    val f2 = new MutableTerminalTreeNode("b", inputB, LineHoldingOffsetInputPosition(line, inputA.size))

    val soo = new SimpleMutableContainerTreeNode("x", Seq(f1, f2), startOf(line), endOf(line))
    soo.pad(line)

    val newInputA = "barnacles"
    soo.dirty should be(false)
    f1.update(newInputA)
    soo.dirty should be(true)
    val updated =
      soo.value
    updated should equal(newInputA + inputB)
  }

  it should "update second scalar match within object value" in {
    val inputA = "foo"
    val inputB = "bar"
    val line = inputA + inputB

    val f1 = new MutableTerminalTreeNode("a", inputA, LineHoldingOffsetInputPosition(line, 0))
    val f2 = new MutableTerminalTreeNode("b", inputB, LineHoldingOffsetInputPosition(line, inputA.size))

    val soo = new SimpleMutableContainerTreeNode("x", Seq(f1, f2), startOf(line), endOf(line))

    val newInputB = "barnacles"
    soo.dirty should be(false)
    f2.update(newInputB)
    soo.dirty should be(true)
    soo.pad(line)
    val updated = soo.value
    updated should equal(inputA + newInputB)
  }

  it should "pad between scalar matches" in {
    val inputA = "foo"
    val inputB = "bar"
    val unmatchedBollocks = "this is bollocks"
    val line = inputA + unmatchedBollocks + inputB

    val f1 = new MutableTerminalTreeNode("a", inputA, LineHoldingOffsetInputPosition(line, 0))
    val f2 = new MutableTerminalTreeNode("b", inputB, LineHoldingOffsetInputPosition(line, inputA.size + unmatchedBollocks.size))

    val soo = SimpleMutableContainerTreeNode.wholeInput("x", Seq(f1, f2), line)

    val newInputB = "barnacles"
    soo.dirty should be(false)
    f2.update(newInputB)
    soo.dirty should be(true)
    val updated = soo.value
    updated should equal(inputA + unmatchedBollocks + newInputB)
  }

  it should "pad first character if not matched" in {
    val inputA = "foo"
    val inputB = "bar"
    val unmatchedBollocks = "this is bollocks"
    val line = " " + inputA + unmatchedBollocks + inputB

    val f1 = new MutableTerminalTreeNode("a", inputA, LineHoldingOffsetInputPosition(line, 1))
    val f2 = new MutableTerminalTreeNode("b", inputB, LineHoldingOffsetInputPosition(line, 1 + inputA.size + unmatchedBollocks.size))

    val soo = SimpleMutableContainerTreeNode.wholeInput("x", Seq(f1, f2), line)
    soo.childNodes.head.nodeName.contains("pad") should be (true)

    val newInputB = "barnacles"
    soo.dirty should be(false)
    f2.update(newInputB)
    soo.dirty should be(true)
    val updated = soo.value
    updated should equal(" " + inputA + unmatchedBollocks + newInputB)
  }

  it should "allow nesting match" in {
    val inputA = "foo"
    val inputB = "bar"
    val inputC = "Lisbon"
    val inputD = "Alentejo"
    val unmatchedBollocks = "this is bollocks"
    val bollocks2 = "(more bollocks)"
    val line = inputA + unmatchedBollocks + inputB + inputC + bollocks2 + inputD

    val f1 = new MutableTerminalTreeNode("a", inputA, LineHoldingOffsetInputPosition(line, 0))
    val f2 = new MutableTerminalTreeNode("b", inputB, LineHoldingOffsetInputPosition(line, inputA.size + unmatchedBollocks.size))

    val ff1 = new MutableTerminalTreeNode("c1", inputC, LineHoldingOffsetInputPosition(line, inputA.size + unmatchedBollocks.size + inputB.size))
    val ff2 = new MutableTerminalTreeNode("c2", inputD, LineHoldingOffsetInputPosition(line, inputA.size + unmatchedBollocks.size + inputB.size + inputC.size + bollocks2.size))

    val f3 = new SimpleMutableContainerTreeNode("c", Seq(ff1, ff2), ff1.startPosition, endOf(line))

    val soo = SimpleMutableContainerTreeNode.wholeInput("x", Seq(f1, f2, f3), line)

    val newInputC = "tentacles"
    soo.dirty should be(false)
    ff1.update(newInputC)
    soo.dirty should be(true)
    val updated = soo.value
    val expected = line.replace(inputC, newInputC)
    updated should equal(expected)
  }

  it should "allow add in nested match" in
    handleLine("")

  it should "allow add in nested match with trailing newline" in
    handleLine("\n")

  it should "allow add in nested match with trailing spaces" in
    handleLine("    ")

  private def handleLine(trailingPadding: String) {
    val inputA = "foo"
    val inputB = "bar"
    val inputC = "Lisbon"
    val inputD = "Alentejo"
    val unmatchedBollocks = "this is bollocks"
    val bollocks2 = "(more bollocks)"
    val line = inputA + unmatchedBollocks + inputB + inputC + bollocks2 + inputD + trailingPadding

    val f1 = new MutableTerminalTreeNode("a", inputA, LineHoldingOffsetInputPosition(line, 0))
    val f2 = new MutableTerminalTreeNode("b", inputB, LineHoldingOffsetInputPosition(line, inputA.size + unmatchedBollocks.size))

    val ff1 = new MutableTerminalTreeNode("c1", inputC, LineHoldingOffsetInputPosition(line, inputA.size + unmatchedBollocks.size + inputB.size))
    val ff2 = new MutableTerminalTreeNode("c2", inputD, LineHoldingOffsetInputPosition(line, inputA.size + unmatchedBollocks.size + inputB.size + inputC.size + bollocks2.size))

    val f3 = new SimpleMutableContainerTreeNode("c", Seq(ff1, ff2), ff1.startPosition, endOf(line))

    val soo = new SimpleMutableContainerTreeNode("full-line", Seq(f1, f2, f3), startOf(line), endOf(line))
    soo.pad(line)

    soo.value should equal(line)

    val newInputD = "tentacles"
    soo.dirty should be(false)
    ff2.update(newInputD)

    soo.value should equal(line.replace(inputD, newInputD))

    f3.appendField(SimpleTerminalTreeNode("what", "dog"))
    soo.dirty should be(true)
    val updated = soo.value
    val expected = line.replace(inputD, newInputD) + "dog"
    updated should equal(expected)
  }
}
