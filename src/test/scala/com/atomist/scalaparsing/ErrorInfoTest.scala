package com.atomist.scalaparsing

import org.scalatest.{Matchers, FlatSpec}

class ErrorInfoTest extends FlatSpec with Matchers {

  it should "not fail on line before 1" in {
    val ei = ErrorInfo(message = "Bad things happened", badInput = "The quick brown fox jumped over the lazy dog",
      line = -1, col = 1, "file/path")
    ei.toString
    // shouldn't fail
  }

  it should "not fail on line 0" in {
    val ei = ErrorInfo(message = "Bad things happened", badInput = "The quick brown fox jumped over the lazy dog",
      line = 0, col = 1, "file/path")
    ei.toString
    // shouldn't fail
  }

  it should "not fail on line out of range" in {
    val ei = ErrorInfo(message = "Bad things happened", badInput = "The quick brown fox \njumped \nover the lazy dog",
      line = 117, col = 1, "file/path")
    ei.toString
    // shouldn't fail
  }

  it should "not fail on 0 column" in {
    val ei = ErrorInfo(message = "Bad things happened", badInput = "The quick brown fox \njumped \nover the lazy dog",
      line = 1, col = 0, "file/path")
    ei.toString
    // shouldn't fail
  }

  it should "not fail on column out of range" in {
    val ei = ErrorInfo(message = "Bad things happened", badInput = "The quick brown fox \njumped \nover the lazy dog",
      line = 1, col = 117, "file/path")
    ei.toString
    // shouldn't fail
  }

}
