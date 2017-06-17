package com.atomist.rug.kind.core

import com.atomist.source.StringFileArtifact
import org.scalatest.{FlatSpec, Matchers}

class LineMutableViewTest extends FlatSpec with Matchers {

  val lineType = new LineType()

  it should "find no lines in empty file" in {
    val emptyFile = StringFileArtifact("thing", "")
    val fmv = FileMutableView(emptyFile, null)
    lineType.findAllIn(fmv).get should be(empty)
  }

  it should "validate 3 line file" in validate(
    "The quick brown fox jumped over the lazy dog",
    "line2",
    "line3"
  )

  it should "validate file with empty line" in validate(
    "The quick brown fox jumped over the lazy dog",
    "",
    "line"
  )

  it should "validate file starting with empty line" in validate(
    "",
    "The quick brown fox jumped over the lazy dog",
    "",
    "line")

  it should "validate file ending with empty line" in {
    val lines = Seq("",
      "The quick brown fox jumped over the lazy dog",
      "",
      "line")
    val f = StringFileArtifact("thing", lines.mkString("\n") + "\n\n")
    val fmv = FileMutableView(f, null)
    val lineViews: Seq[LineMutableView] = lineType.findAllIn(fmv).get
    assert(lineViews.size === lines.size + 1)
    lineViews.zipWithIndex.foreach(tup => {
      if (tup._2 < lines.size) assert(tup._1.value === lines(tup._2))
      assert(tup._1.num === tup._2)
      assert(tup._1.numFrom1 === tup._2 + 1)
    })
  }

  private def validate(lines: String*) {
    val f = StringFileArtifact("thing", lines.mkString("\n"))
    val fmv = FileMutableView(f, null)
    val lineViews: Seq[LineMutableView] = lineType.findAllIn(fmv).get
    assert(lineViews.size === lines.size)
    lineViews.zipWithIndex.foreach(tup => {
      assert(tup._1.value === lines(tup._2))
      assert(tup._1.num === tup._2)
      assert(tup._1.numFrom1 === tup._2 + 1)
    })
  }

}
