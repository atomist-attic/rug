package com.atomist.tree.content.text

import org.scalatest.{FlatSpec, Matchers}

class LineInputPositionImplTest extends FlatSpec with Matchers {

  it should "find position at beginning of file" in {
    val input = "The quick brown fox jumped over the lazy dog"
    val p = LineInputPositionImpl(input, 0, 1)
    assert(p.offset === 0)
    val p2 = LineInputPositionImpl(input, -1, 1)
    assert(p2.offset === 0)
    input(p2.offset) // shouldn't throw exception

    val p3 = LineInputPositionImpl(input, -1, 10)
    assert(p3.offset === 0)
    input(p3.offset) // shouldn't throw exception
  }

  it should "find position at start of single line" in {
    val input = "The quick brown fox jumped over the lazy dog"
    val p = LineInputPositionImpl(input, 1, 1)
    assert(p.offset === 0)
    assert(input(p.offset) === 'T')
  }

  it should "find position after start of single line" in {
    val input = "The quick brown fox jumped over the lazy dog"
    val p = LineInputPositionImpl(input, 1, 2)
    assert(p.offset === 1)
    assert(input(p.offset) === 'h')
  }

  it should "find position within single line" in {
    val input = "The quick brown fox jumped over the lazy dog"
    val p = LineInputPositionImpl(input, 1, 5)
    assert(p.offset === 4)
  }

  it should "find position at beginning of second line" in {
    val input = "The\nAnd this is a test"
    val p = LineInputPositionImpl(input, 2, 1)
    assert(p.offset === 4)
  }

  it should "find position at end of input" in {
    val input = "The\nAnd this is a test"
    val p = LineInputPositionImpl(input, 200, 1)
    assert(p.offset === input.length - 1)
    assert(input(p.offset) === 't')
  }

  it should "show marker" in {
    val input = "The\nAnd this is a test"
    val p = LineInputPositionImpl(input, 2, 1)
    assert(p.offset === 4)
    p.show.contains("And this") should be (true)
    assert(input(p.offset) === 'A')
  }

}
