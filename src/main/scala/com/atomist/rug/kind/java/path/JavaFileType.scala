package com.atomist.rug.kind.java.path

import com.atomist.rug.kind.grammar.AntlrRawFileType
import com.atomist.source.FileArtifact
import com.atomist.tree.TreeNode
import com.atomist.tree.content.text.grammar.antlr.{AstNodeCreationStrategy, FromGrammarAstNodeCreationStrategy}


object FromJavaGrammarAstNodeCreationStrategy extends AstNodeCreationStrategy {

  override def nameForContainer(rule: String, fields: Seq[TreeNode]): String = rule

  override def tagsForContainer(rule: String, fields: Seq[TreeNode]): Set[String] = rule match {
    case "classDeclaration" => Set("Class", "classDeclaration")
    case "methodDeclaration" => Set("Method", "methodDeclaration")
    case "importDeclaration" => Set("Import", "importDeclaration")
    case "lambdaExpression" => Set("Lambda", "lambdaExpression")
    case "formalParameter" => Set("Args", "formalParameter")
    case "lastFormalParameter" => Set("VarArgs", "lastFormalParameter")
    case r => Set(r)
  }

  override def significance(rule: String, fields: Seq[TreeNode]): TreeNode.Significance =
    TreeNode.Signal
}

/**
  * Path-expression oriented Java type built on JavaParser
  */
class JavaFileType
  extends AntlrRawFileType("compilationUnit",
    FromJavaGrammarAstNodeCreationStrategy,
    "classpath:grammars/antlr/Java8.g4") {

  override def description = "Java file"

  override def isOfType(f: FileArtifact): Boolean =
    f.name.endsWith(".java")

}
