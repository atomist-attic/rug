package com.atomist.tree.content.text

import org.scalatest.{FlatSpec, Matchers}

class MutableTerminalTreeNodeTest extends FlatSpec with Matchers {

  it should "update at start of line" in {
    val initialInput = "Cats are cool"
    val sm = new MutableTerminalTreeNode("animal", "Cat", LineInputPositionImpl(initialInput, 1, 1))
    sm.update("Dog")
    sm.value should equal("Dog")
  }

  it should "update in line" in {
    val initialInput = "Some cats are cool"
    val sm = new MutableTerminalTreeNode("animal", "cat", LineInputPositionImpl(initialInput, 1, 6))
    sm.update("dog")
    sm.value should equal("dog")
  }

  it should "update empty string at end of input" in {
    val initialInput = "Some cats are cool"
    val sm = new MutableTerminalTreeNode("animal", "", LineInputPositionImpl(initialInput, 1, initialInput.size))
    sm.update("er than others")
    sm.value should equal("er than others")
  }

}
