package com.atomist.rug.kind.csharp

import com.atomist.rug.kind.grammar.AntlrRawFileType
import com.atomist.source.FileArtifact
import com.atomist.tree.TreeNode
import com.atomist.tree.content.text.grammar.antlr.{AstNodeCreationStrategy, FromGrammarAstNodeCreationStrategy}
import com.atomist.tree.pathexpression.{PathExpression, PathExpressionParser}

object FromCSharpGrammarAstNodeCreationStrategy extends AstNodeCreationStrategy {

  override def nameForContainer(rule: String, fields: Seq[TreeNode]): String = rule

  override def tagsForContainer(rule: String, fields: Seq[TreeNode]): Set[String] = rule match {
    case "class_definition" => Set("Class", "class_definition")
    case "method_declaration" => Set("Method", "method_declaration")
    case "using_directive" => Set("Using", "using_directive", "Import")
    case "lambda_expression" => Set("Lambda", "lambda_expression")
    case "fixed_parameter" => Set("Args", "fixed_parameter")
    case "parameter_array" => Set("VarArgs", "parameter_array")
    case r => Set(r)
  }

  override def significance(rule: String, fields: Seq[TreeNode]): TreeNode.Significance =
    TreeNode.Signal
}

object CSharpFileType {

  val CSharpExtension = ".cs"
}

class CSharpFileType
  extends AntlrRawFileType(topLevelProduction = "compilation_unit",
    FromCSharpGrammarAstNodeCreationStrategy,
    "classpath:grammars/antlr/CSharpLexer.g4",
    "classpath:grammars/antlr/CSharpParser.g4"
  ) {

  import CSharpFileType._

  override def description = "C# file"

  override def isOfType(f: FileArtifact): Boolean =
    f.name.endsWith(CSharpExtension)

}

object CSharpFileMutableView {

  import PathExpressionParser.parseString

  val FirstUsingStatement: PathExpression = "//using_directive[1]"
}
