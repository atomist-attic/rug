package com.atomist.rug.kind.java.path

import com.atomist.rug.kind.grammar.AntlrRawFileType
import com.atomist.source.FileArtifact
import com.atomist.tree.content.text.grammar.antlr.FromGrammarAstNodeCreationStrategy

/**
  * Path-expression oriented Java type built on JavaParser.
  */
class JavaFileType
  extends AntlrRawFileType("compilationUnit",
    FromGrammarAstNodeCreationStrategy,
    "classpath:grammars/antlr/Java8.g4") {

  override def description = "Java file"

  override def isOfType(f: FileArtifact): Boolean =
    f.name.endsWith(".java")

}
