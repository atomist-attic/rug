package com.atomist.rug.parser

import com.atomist.rug.BadRugSyntaxException
import com.atomist.source.StringFileArtifact
import org.scalatest.{FlatSpec, Matchers}

class ParsingErrorReportingTest extends FlatSpec with Matchers {

  val ri: RugParser = new ParserCombinatorRugParser()

  it should "give good error message for non reserved word" in {
    val prog =
      s"""
         |@description '100% JavaScript free'
         |editor Triplet
         |
         |le t
         |  Bar = "bad identifier name"
         |
         |with file f
         | when isJava
         |
         |do
         | append "foobar"
      """.stripMargin
    try {
      ri.parse(prog)
      fail
    }
    catch {
      case b: BadRugSyntaxException =>
        b.getMessage.contains("l") should be(true)
        b.info.line should be(5)
        b.info.col should be(1)
        b.info. filePath should be(RugParser.DefaultRugPath)
    }
  }

  it should "produce good error message for bad let syntax" in {
    val prog =
      s"""
         |@description '100% JavaScript free'
         |editor Triplet
         |
         |le t
         |  Bar = "bad identifier name"
         |
         |with file f
         | when isJava
         |
         |do
         | append "foobar"
      """.stripMargin
    val f = StringFileArtifact("editors/test.rug", prog)
    try {
      ri.parse(f)
      fail
    }
    catch {
      case b: BadRugSyntaxException =>
        b.getMessage.contains("le t") should be(true)
        b.info.line should be(5)
        b.info.col should be <(3)
        b.info.filePath should equal(f.path)
        b.getMessage.startsWith(f.path + ":") should be(true)
    }
  }

  it should "report bad reference" in {
    val prog =
      s"""
         |@description '100% JavaScript free'
         |editor Triplet
         |
         |param dog: @does_not_exist
         |
         |with file f
         | when isJava
         |
         |do
         | append "foobar"
      """.stripMargin
    val f = StringFileArtifact("editors/test.rug", prog)
    try {
      ri.parse(f)
      fail
    }
    catch {
      case b: BadRugSyntaxException =>
        b.getMessage.contains("does_not_exist") should be(true)
        b.getMessage.startsWith(f.path + ":") should be(true)
    }
  }
}
