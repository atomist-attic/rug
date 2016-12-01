package com.atomist.util.lang

/**
  * Useful helper for generating any language
  *
  * @param indent one indent: E.g. a number of spaces or tabs
  */
class CodeGenerationHelper(indent: String = "    ") {

  /**
    * Indent the block
    *
    * @param block
    * @param n number of indents
    * @return a String
    */
  def indented(block: String, n: Int): String = {
    val padding = Array.fill[String](n)(indent).mkString("")
    padding + block.replace("\n", s"\n$padding")
  }

  /**
    * Wrap the given block in an outer block. E.g. wrapping a block in an if
    * statement.
    *
    * @param outerLeft left part of the string e.g. "if (cond) {\n"
    * @param outerRight right part e.g. "\n}"
    * @param block block to wrap
    * @param n indent depth to use for block
    * @return a String
    */
  def wrap(outerLeft: String, outerRight: String, block: String, n: Int): String = {
    outerLeft + indented(block, n) + outerRight
  }

}
