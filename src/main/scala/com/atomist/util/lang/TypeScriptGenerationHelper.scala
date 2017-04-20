package com.atomist.util.lang

import com.atomist.rug.runtime.js.interop.jsPathExpressionEngine
import com.atomist.rug.spi.{EnumParameterOrReturnType, ParameterOrReturnType, TypeRegistry}

/**
  * Useful helpers for generating TypeScript.
  *
  * @param indent one indent: E.g. a number of spaces or tabs
  */
class TypeScriptGenerationHelper(indent: String = "    ")
  extends CodeGenerationHelper(indent) {

  /**
    * Convert the block to a JsDoc style comment.
    */
  def toJsDoc(block: String): String = {
    s"""|/*
        | * ${block.replace("\n", "\n * ")}
        | */""".stripMargin
  }

  def rugTypeToTypeScriptType(jt: ParameterOrReturnType, tr: TypeRegistry): String = {
    val pathExpressionEngineClassName = "class " + classOf[jsPathExpressionEngine].getName
    jt.name match {
      case "String" => "string"
      case "boolean" => "boolean"
      case "int" | "long" => "number"
      case "void" => "void"
      case "java.util.List<java.lang.String>" => "string[]"
      case "class java.lang.String" => "string"
      case "Object" => "any"
      case "class java.lang.Object" => "any"
      case "java.util.List<com.atomist.rug.kind.core.ProjectMutableView>" => "Project[]"
      case "class com.atomist.rug.kind.core.ProjectMutableView" => "Project"
      case "java.util.List<java.lang.Object>" => "any[]"
      case "class com.atomist.rug.kind.core.FileMutableView" => "File"
      case "class com.atomist.rug.kind.core.ProjectContext" => "ProjectContext"
      case "scala.collection.immutable.List<java.lang.Object>" => "any[]"
      case "java.util.List<com.atomist.rug.kind.core.FileArtifactBackedMutableView>" => "File[]"
      case "java.util.List<com.atomist.rug.kind.java.support.PackageInfo>" => "any[]" //TODO
      case "java.util.List<com.atomist.rug.kind.service.ServiceMutableView>" => "any[]"
      case "java.util.List<com.atomist.tree.TreeNode>" => "any[]"
      case "interface com.atomist.graph.GraphNode" =>
        // TODO for some reason, the correct return of "GraphNode" won't compile although it's imported
        "TreeNode"
      case "interface com.atomist.tree.TreeNode" => "TreeNode"
      case "List" => "any[]" // TODO improve this
      case "FileArtifactMutableView" => "File" // TODO this is nasty
      case "scala.collection.immutable.Set<java.lang.String>" => "string[]" // Nasty
      case `pathExpressionEngineClassName` => "PathExpressionEngine"
      case "class com.atomist.tree.content.text.FormatInfo" => "FormatInfo"
      case _ if jt.isInstanceOf[EnumParameterOrReturnType] =>
        jt.asInstanceOf[EnumParameterOrReturnType].legalValues
          .map("\"" + _ + "\"")
          .mkString(" | ")
      case x if x.endsWith("MutableView") && x.contains(".") =>
        val className = x.substring(x.lastIndexOf(".") + 1)
        val cname = className.stripSuffix("MutableView")
        cname
      case x if tr.findByName(x).isDefined && jt.isArray => x + "[]"
      case x if tr.findByName(x).isDefined => x
      case x =>
        throw new UnsupportedOperationException(s"Unsupported type [$x]. Did you export a function with this in its type signature?")
    }
  }
}
