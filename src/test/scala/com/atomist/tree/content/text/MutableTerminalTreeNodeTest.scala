package com.atomist.tree.content.text

import org.scalatest.{FlatSpec, Matchers}

class MutableTerminalTreeNodeTest extends FlatSpec with Matchers {

  it should "update at start of line" in {
    val initialInput = "Cats are cool"
    val sm = new MutableTerminalTreeNode("animal", "Cat", LineInputPositionImpl(initialInput, 1, 1))
    sm.update("Dog")
    assert(sm.value === "Dog")
  }

  it should "update in line" in {
    val initialInput = "Some cats are cool"
    val sm = new MutableTerminalTreeNode("animal", "cat", LineInputPositionImpl(initialInput, 1, 6))
    sm.update("dog")
    assert(sm.value === "dog")
  }

  it should "update empty string at end of input" in {
    val initialInput = "Some cats are cool"
    val sm = new MutableTerminalTreeNode("animal", "", LineInputPositionImpl(initialInput, 1, initialInput.length))
    sm.update("er than others")
    assert(sm.value === "er than others")
  }

}
