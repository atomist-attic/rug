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
    s"""|/**
        | * ${block.replace("\n", "\n * ")}
        | */""".stripMargin
  }

  def rugTypeToTypeScriptType(jt: ParameterOrReturnType, tr: TypeRegistry): String = {
    val pathExpressionEngineClassName = classOf[jsPathExpressionEngine].getName
    val typeName = jt.name match {
      case "String" => "string"
      case "boolean" => "boolean"
      case "int" | "long" => "number"
      case "void" => "void"
      case "java.lang.String" => "string"
      case "Object" => "any"
      case "java.lang.Object" => "any"
      case "scala.collection.immutable.List<java.lang.Object" => "any"
      case "com.atomist.rug.kind.java.support.PackageInfo" => "any" //TODO
      case "com.atomist.graph.GraphNode" =>
        // TODO for some reason, the correct return of "GraphNode" won't compile although it's imported
        "TreeNode"
      case "FileArtifactMutableView" => "File"
      case `pathExpressionEngineClassName` => "PathExpressionEngine"
      case _ if jt.isInstanceOf[EnumParameterOrReturnType] =>
        jt.asInstanceOf[EnumParameterOrReturnType].legalValues
          .map("\"" + _ + "\"")
          .mkString(" | ")
      case x if x.endsWith("MutableView") && x.contains(".") =>
        val className = x.substring(x.lastIndexOf(".") + 1)
        val cname = className.stripSuffix("MutableView")
        cname
      case x if tr.findByName(x).isDefined => x
      case x if x.contains(".") =>
        x.substring(x.lastIndexOf(".") + 1)
      case x =>
        throw new UnsupportedOperationException(s"Unsupported type [$x]. Did you export a function with this in its type signature?")
    }
    typeName + (if (jt.isArray) "[]" else "")
  }
}
