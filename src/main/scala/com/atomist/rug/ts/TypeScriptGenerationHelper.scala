package com.atomist.rug.ts

import com.atomist.rug.kind.java.support.JavaHelpers

/**
  * Useful helps for generating TypeScript
  *
  * @param indent one indent: E.g. a number of spaces or tabs
  */
class TypeScriptGenerationHelper(indent: String = "    ")
  extends CodeGenerationHelper(indent) {

  /**
    * Convert the block to a JsDoc style comment
    *
    * @param block
    * @return
    */
  def toJsDoc(block: String) = {
    s"""
       |/**
       |   ${indented(block, 1)}
       |*/""".stripMargin
  }

  def javaTypeToTypeScriptType(jt: String): String = {
    jt match {
      case "String" => "string"
      case "boolean" => "boolean"
      case "int" | "long" => "number"
      case "void" => "void"
      case "Object" => "any"
      case "List" => "any[]" // TODO improve this
      case x => throw new UnsupportedOperationException(s"Unsupported type [$jt]")
    }
  }

  def typeScriptClassNameForTypeName(name: String) = {
    JavaHelpers.toJavaClassName(name)
  }
}
