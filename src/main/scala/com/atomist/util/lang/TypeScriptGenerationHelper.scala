package com.atomist.util.lang

import com.atomist.rug.runtime.js.interop.jsPathExpressionEngine

/**
  * Useful helps for generating TypeScript
  *
  * @param indent one indent: E.g. a number of spaces or tabs
  */
class TypeScriptGenerationHelper(indent: String = "    ")
  extends CodeGenerationHelper(indent) {

  /**
    * Convert the block to a JsDoc style comment.
    */
  def toJsDoc(block: String): String = {
    s"""
       |/**
       |   ${indented(block, 1)}
       |*/""".stripMargin
  }

  def javaTypeToTypeScriptType(jt: String): String = {
    val pathExpressionEngineClassName = "class " + classOf[jsPathExpressionEngine].getName
    jt match {
      case "String" => "string"
      case "boolean" => "boolean"
      case "int" | "long" => "number"
      case "void" => "void"
      case "java.util.List<java.lang.String>" => "string[]"
      case "class java.lang.String" => "string"
      case "Object" => "any"
      case "class java.lang.Object" =>  "any"
      case "java.util.List<com.atomist.rug.kind.core.ProjectMutableView>" => "Project[]"
      case "class com.atomist.rug.kind.core.ProjectMutableView" => "Project"
      case "java.util.List<java.lang.Object>" => "any[]"
      case "class com.atomist.rug.kind.core.FileMutableView" => "File"
      case "class com.atomist.rug.kind.core.ProjectContext" => "ProjectContext"
      case "scala.collection.immutable.List<java.lang.Object>" => "any[]"
      case "java.util.List<com.atomist.rug.kind.core.FileArtifactBackedMutableView>" => "File[]"
      case "java.util.List<com.atomist.rug.kind.java.support.PackageInfo>" => "any[]"//TODO
      case "java.util.List<com.atomist.rug.kind.service.ServiceMutableView>" => "any[]"
      case "java.util.List<com.atomist.tree.TreeNode>" => "any[]"
      case "List" => "any[]" // TODO improve this
      case "FileArtifactMutableView" => "File"   // TODO this is nasty
      case "scala.collection.immutable.Set<java.lang.String>" => "string[]" // Nasty
      case `pathExpressionEngineClassName` => "PathExpressionEngine"
      case "class com.atomist.tree.content.text.FormatInfo" => "FormatInfo"
      case x => throw new UnsupportedOperationException(s"Unsupported type [$jt]")
    }
  }
}
