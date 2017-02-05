package com.atomist.rug.kind.grammar

import com.atomist.tree.content.text._
import com.atomist.tree.{SimpleTerminalTreeNode, TreeNode}
import org.scalatest.{FlatSpec, Matchers}

class SimpleMutableContainerTreeNodeTest extends FlatSpec with Matchers {

  import OffsetInputPosition._

  it should "update first scalar match within object value" in {
    val inputA = "foo"
    val inputB = "bar"
    val line = inputA + inputB

    val f1 = new MutableTerminalTreeNode("a", inputA, LineHoldingOffsetInputPosition(line, 0))
    val f2 = new MutableTerminalTreeNode("b", inputB, LineHoldingOffsetInputPosition(line, inputA.length))

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
    val f2 = new MutableTerminalTreeNode("b", inputB, LineHoldingOffsetInputPosition(line, inputA.length))

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
    val unmatchedContent = "this is incorrect"
    val line = inputA + unmatchedContent + inputB

    val f1 = new MutableTerminalTreeNode("a", inputA, LineHoldingOffsetInputPosition(line, 0))
    val f2 = new MutableTerminalTreeNode("b", inputB, LineHoldingOffsetInputPosition(line, inputA.length + unmatchedContent.length))

    val soo = SimpleMutableContainerTreeNode.wholeInput("x", Seq(f1, f2), line)

    val newInputB = "barnacles"
    soo.dirty should be(false)
    f2.update(newInputB)
    soo.dirty should be(true)
    val updated = soo.value
    updated should equal(inputA + unmatchedContent + newInputB)
  }

  it should "pad first character if not matched" in {
    val inputA = "foo"
    val inputB = "bar"
    val unmatchedContent = "this is incorrect"
    val line = " " + inputA + unmatchedContent + inputB

    val f1 = new MutableTerminalTreeNode("a", inputA, LineHoldingOffsetInputPosition(line, 1))
    val f2 = new MutableTerminalTreeNode("b", inputB, LineHoldingOffsetInputPosition(line, 1 + inputA.length + unmatchedContent.length))

    val soo = SimpleMutableContainerTreeNode.wholeInput("x", Seq(f1, f2), line)
    soo.fieldValues.head.nodeName.contains("pad") should be(true)
    soo.childNodes.head.nodeName.contains("pad") should be(false)

    val newInputB = "barnacles"
    soo.dirty should be(false)
    f2.update(newInputB)
    soo.dirty should be(true)
    val updated = soo.value
    updated should equal(" " + inputA + unmatchedContent + newInputB)
  }

  it should "allow nesting match" in {
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

    val newInputC = "tentacles"
    soo.dirty should be(false)
    ff1.update(newInputC)
    soo.dirty should be(true)
    val updated = soo.value
    val expected = line.replace(inputC, newInputC)
    updated should equal(expected)
  }

  it should "find format info from nested node" in {
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

    soo.formatInfo(f1).get.start.offset should equal (f1.startPosition.offset)
    soo.formatInfo(f2).get.start.offset should equal (f2.startPosition.offset)
    soo.formatInfo(ff1).get.start.offset should equal (ff1.startPosition.offset)
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
    val unmatchedContent = "this is incorrect"
    val moreContent = "(more content)"
    val line = inputA + unmatchedContent + inputB + inputC + moreContent + inputD + trailingPadding

    val f1 = new MutableTerminalTreeNode("f1", inputA, LineHoldingOffsetInputPosition(line, 0))
    val f2 = new MutableTerminalTreeNode("f2", inputB, LineHoldingOffsetInputPosition(line, inputA.length + unmatchedContent.length))

    val ff1 = new MutableTerminalTreeNode("ff1", inputC, LineHoldingOffsetInputPosition(line, inputA.length + unmatchedContent.length + inputB.length))
    val ff2 = new MutableTerminalTreeNode("ff2", inputD, LineHoldingOffsetInputPosition(line, inputA.length + unmatchedContent.length + inputB.length + inputC.length + moreContent.length))

    val f3 = new SimpleMutableContainerTreeNode("f3", Seq(ff1, ff2), ff1.startPosition, endOf(line),
      significance = TreeNode.Signal)

    val topLevel = new SimpleMutableContainerTreeNode("full-line", Seq(f1, f2, f3), startOf(line), endOf(line))
    topLevel.pad(line)

    topLevel.value should equal(line)

    val newInputD = "tentacles"
    topLevel.dirty should be(false)
    ff2.update(newInputD)

    topLevel.value should equal(line.replace(inputD, newInputD))

    f3.appendField(SimpleTerminalTreeNode("what", "dog"))

    topLevel.dirty should be(true)
    //println(s"${TreeNodeUtils.toShortString(soo)}")
    val updated = topLevel.value
    val expected = line.replace(inputD, newInputD) + "dog"
    updated should equal(expected)
  }

  it should "pull up grandchildren of noise container" in
    pullUpGrandKids("woeiruwoeiurowieurowiuer")

  private def pullUpGrandKids(trailingPadding: String) {
    val inputA = "foo"
    val inputB = "bar"
    val inputC = "Lisbon"
    val inputD = "Alentejo"
    val unmatchedContent = "this is incorrect"
    val moreContent = "(more content)"
    val line = inputA + unmatchedContent + inputB + inputC + moreContent + inputD + trailingPadding

    val ff1 = new MutableTerminalTreeNode("ff1", inputC, LineHoldingOffsetInputPosition(line, inputA.length + unmatchedContent.length + inputB.length))
    val ff2 = new MutableTerminalTreeNode("ff2", inputD, LineHoldingOffsetInputPosition(line, inputA.length + unmatchedContent.length + inputB.length + inputC.length + moreContent.length))

    val f3 = new SimpleMutableContainerTreeNode("f3", Seq(ff1, ff2), ff1.startPosition, endOf(line),
      significance = TreeNode.Noise)

    val topLevel = new SimpleMutableContainerTreeNode("full-line", Seq(f3), startOf(line), endOf(line), significance = TreeNode.Signal)
    topLevel.fieldValues.contains(ff2) should be(false)
    topLevel.pad(line)

    topLevel.value should equal(line)

    val newInputD = "tentacles"
    topLevel.dirty should be(false)
    ff2.update(newInputD)

    topLevel.value should equal(line.replace(inputD, newInputD))

    withClue("child nodes should have been pulled up") {
      topLevel.fieldValues.contains(ff1) should be(true)
      topLevel.fieldValues.contains(ff2) should be(true)
    }

    //    f3.appendField(SimpleTerminalTreeNode("what", "dog"))
    //
    //    topLevel.dirty should be(true)
    //    //println(s"${TreeNodeUtils.toShortString(soo)}")
    //    val updated = topLevel.value
    //    val expected = line.replace(inputD, newInputD) + "dog"
    //    updated should equal(expected)
  }

  it should "pull up great-grandchildren of noise container" in
    pullUpGreatGrandKids("woeiruwoeiurowieurowiuer")

  private def pullUpGreatGrandKids(trailingPadding: String) {
    val inputA = "foo"
    val inputB = "bar"
    val inputC = "Lisbon"
    val inputD = "Alentejo"
    val unmatchedContent = "this is incorrect"
    val moreContent = "(more content)"
    val line = inputA + unmatchedContent + inputB + inputC + moreContent + inputD + trailingPadding

    val ff1 = new MutableTerminalTreeNode("ff1", inputC, LineHoldingOffsetInputPosition(line, inputA.length + unmatchedContent.length + inputB.length))
    val ff2 = new MutableTerminalTreeNode("ff2", inputD, LineHoldingOffsetInputPosition(line, inputA.length + unmatchedContent.length + inputB.length + inputC.length + moreContent.length))

    val extraNoiseLayer = new SimpleMutableContainerTreeNode("extraNoiseLayer", Seq(ff1, ff2), ff1.startPosition, endOf(line),
      significance = TreeNode.Noise)

    val f3 = new SimpleMutableContainerTreeNode("f3", Seq(extraNoiseLayer), ff1.startPosition, endOf(line),
      significance = TreeNode.Noise)

    val topLevel = new SimpleMutableContainerTreeNode("full-line", Seq(f3), startOf(line), endOf(line), significance = TreeNode.Signal)
    topLevel.fieldValues.contains(ff2) should be(false)
    topLevel.pad(line)

    topLevel.value should equal(line)

    val newInputD = "tentacles"
    topLevel.dirty should be(false)
    ff2.update(newInputD)

    // topLevel.value should equal(line.replace(inputD, newInputD))

    withClue("child nodes should have been pulled up") {
      topLevel.fieldValues.contains(ff1) should be(true)
      topLevel.fieldValues.contains(ff2) should be(true)
      topLevel.childNodes.contains(ff1) should be(true)
      topLevel.childNodes.contains(ff2) should be(true)
    }
  }
}
