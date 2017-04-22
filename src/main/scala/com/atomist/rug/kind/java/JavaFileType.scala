package com.atomist.rug.kind.java

import com.atomist.rug.kind.grammar.AntlrRawFileType
import com.atomist.source.FileArtifact
import com.atomist.tree.content.text.grammar.antlr.FromGrammarAstNodeCreationStrategy

/**
  * Path-expression oriented Java type built on JavaParser.
  * Please refer to the ANTLR grammar for Java 8 for the
  * production names, which are used unchanged in path expression.
  * https://github.com/antlr/grammars-v4/blob/master/java8/Java8.g4
  */
class JavaFileType
  extends AntlrRawFileType("compilationUnit",
    FromGrammarAstNodeCreationStrategy,
    "classpath:grammars/antlr/Java8.g4") {

  override def description = "Java file"

  override def isOfType(f: FileArtifact): Boolean =
    f.name.endsWith(".java")

}
