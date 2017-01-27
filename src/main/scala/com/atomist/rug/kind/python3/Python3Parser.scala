package com.atomist.rug.kind.python3

import java.nio.charset.StandardCharsets

import com.atomist.tree.content.text.grammar.antlr.AntlrGrammar
import com.atomist.tree.content.text.grammar.{MatchListener, Parser}
import com.atomist.tree.content.text.MutableContainerTreeNode
import com.atomist.tree.content.text.TreeNodeOperations._
import com.atomist.util.Utils.withCloseable
import org.apache.commons.io.IOUtils
import org.springframework.core.io.DefaultResourceLoader



object Python3Parser {

  val PythonReservedWords = Set(
    "class", "finally", "is", "return",
    "continue", "for", "lambda", "try",
    "def", "from", "nonlocal", "while",
    "and", "del", "global", "not", "with",
    "as", "elif", "if", "or", "yield",
    "assert", "else", "import", "pass",
    "break", "except", "in", "raise"
  )
}