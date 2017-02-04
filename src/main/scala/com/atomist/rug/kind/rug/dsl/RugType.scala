package com.atomist.rug.kind.rug.dsl

import com.atomist.rug.kind.grammar.AntlrRawFileType
import com.atomist.source.FileArtifact
import com.atomist.tree.content.text.grammar.antlr.FromGrammarAstNodeCreationStrategy

/**
  * Exposes an ANTLR-based AST for Rug DSL files.
  */
class RugFileType
  extends AntlrRawFileType(
    topLevelProduction = "rug_file",
    nodeCreationStrategy = FromGrammarAstNodeCreationStrategy,
    grammars = "classpath:grammars/antlr/Rug.g4") {

  override def description = "Rug DSL file"

  override def isOfType(f: FileArtifact): Boolean =
    f.name.endsWith(".rug")

}
