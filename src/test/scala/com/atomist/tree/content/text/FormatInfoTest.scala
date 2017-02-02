package com.atomist.tree.content.text

import org.scalatest.{FlatSpec, Matchers}

class FormatInfoTest extends FlatSpec with Matchers {

  import FormatInfo.contextInfo

  it should "find position at start of file" in {
    val fi = contextInfo("")
    fi.offset should be (0)
    fi.columnNumberFrom1 should be (1)
    fi.lineNumberFrom1 should be (1)
    fi.indentDepth should be (0)
  }

  it should "find position within first line of file" in {
    val input = "The quick brown"
    val fi = contextInfo(input)
    fi.columnNumberFrom1 should be (input.length + 1)
    fi.lineNumberFrom1 should be (1)
    fi.indentDepth should be (0)
  }

  it should "find position in second line of file" in {
    val input = "The\n "
    val fi = contextInfo(input)
    fi.columnNumberFrom1 should be (2)
    fi.lineNumberFrom1 should be (2)
    fi.indentDepth should be (1)
  }

  it should "recognize indent depth of 4 with spaces" in
    testIndent("    ")

  it should "recognize indent depth of 3 with spaces" in {
    val fi = testIndent("   ")
    fi.usesTabs should be (false)

  }

  it should "recognize indent depth of 1 with tabs" in {
    val fi = testIndent("\t")
    fi.usesTabs should be (true)
  }

  private def testIndent(indent: String): PointFormatInfo = {
    val input =
      s"""
        |public class Foobar {
        |${indent}int i;
        |
        |${indent}void doSomething() {
        |${indent}${indent}doIt();""".stripMargin
    val fi = contextInfo(input)
    fi.columnNumberFrom1 should be (2 * indent.length + 8)
    fi.lineNumberFrom1 should be (6)
    fi.indentDepth should be (2)
    fi.indent should be (indent)
    fi
  }

  it should "add indented content with 4 spaces" in
    testAddContent("    ", "Whatever")

  it should "add indented content with tab" in
    testAddContent("\t", "Whatever")

  private def testAddContent(indent: String, content: String): String = {
    val input =
      s"""
         |public class Foobar {
         |${indent}int i;
         |
        |${indent}void doSomething() {
         |${indent}${indent}doIt();""".stripMargin
    val fi = contextInfo(input)
    fi.indent should be (indent)
    fi.indentDepth should be (2)
    val indented = fi.indented(content)
    indented.take(indent.length + 1) should equal ("\n" + indent)
    val r = input + fi.indented(content)
    r should equal(input + "\n" + indent + indent + content)
    r
  }

  it should "add multi line content" in {
    val indent = "   "
    val content =
      s"""public Thing {
        |\tdoIt();""".stripMargin
    val input =
      s"""
         |public class Foobar {
         |${indent}int i;
         |
         |${indent}void doSomething() {
         |${indent}${indent}doIt();""".stripMargin
    val fi = contextInfo(input)
    fi.indent should be (indent)
    fi.indentDepth should be (2)
    val indented = fi.indented(content)
    indented.contains("\t") should be (false)
    indented.take(indent.length + 1) should equal ("\n" + indent)
    val r = input + fi.indented(content)
    r.contains("\t") should be (false)
    r should equal(input + "\n" +
      indent + indent + "public Thing {\n" + indent + indent + indent + "doIt();")
    r
  }

}
