package com.atomist.tree.content.text

import org.scalatest.{FlatSpec, Matchers}

class LineInputPositionImplTest extends FlatSpec with Matchers {

  it should "find position at start of single line" in {
    val input = "The quick brown fox jumped over the lazy dog"
    val p = LineInputPositionImpl(input, 1, 1)
    p.offset should be (0)
  }

  it should "find position within single line" in {
    val input = "The quick brown fox jumped over the lazy dog"
    val p = LineInputPositionImpl(input, 1, 5)
    p.offset should be (4)
  }

  it should "find position at beginning of second line" in {
    val input = "The\nAnd this is a test"
    val p = LineInputPositionImpl(input, 2, 1)
    p.offset should be (4)
  }

  it should "show marker" in {
    val input = "The\nAnd this is a test"
    val p = LineInputPositionImpl(input, 2, 1)
    p.offset should be (4)
    p.show.contains("And this") should be (true)
  }

}
