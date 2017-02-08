package com.atomist.tree.content.text

import org.scalatest.{FlatSpec, Matchers}

class InputPositionTest extends FlatSpec with Matchers {

  it should "select range" in {
    val input = "The quick brown fox jumped over the lazy dog"
    val start = LineInputPositionImpl(input, 1, 1)
    val end = LineInputPositionImpl(input, 1, 2)

    start.takeTo(input, end) should be ("T")
  }

}
