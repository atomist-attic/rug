package com.atomist.rug.kind.python3

import com.atomist.rug.kind.grammar.AntlrRawFileType
import com.atomist.source.FileArtifact
import com.atomist.tree.{ContainerTreeNode, TreeNode}
import com.atomist.tree.TreeNode.Significance
import com.atomist.tree.content.text.grammar.antlr.AstNodeCreationStrategy

object PythonFileType {

  val PythonExtension = ".py"

}

object FromPythonGrammarAstNodeCreationStrategy extends AstNodeCreationStrategy {

  override def nameForContainer(rule: String, fields: Seq[TreeNode]): String = rule

  override def tagsForContainer(rule: String, fields: Seq[TreeNode]): Set[String] = rule match {
    case "classdef" => Set("Class", "classdef")
    case "funcdef" => Set("Func", "funcdef")
    case "import_stmt" => Set("Import", "import_stmt")
    case "lambdef" => Set("Lambda", "lambdef")
    case "tfpdef" => Set("Args", "tfpdef")
    case r => Set(r)
  }

  override def significance(rule: String, fields: Seq[TreeNode]): TreeNode.Significance =
    TreeNode.Signal
}


class PythonFileType
  extends AntlrRawFileType("file_input",
    FromPythonGrammarAstNodeCreationStrategy,
    "classpath:grammars/antlr/Python3.g4") {

  import PythonFileType._

  override def description = "Python file"

  override def isOfType(f: FileArtifact): Boolean =
    f.name.endsWith(PythonExtension)

}
