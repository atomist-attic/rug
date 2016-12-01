package com.atomist.rug.kind.python3

import java.nio.charset.StandardCharsets

import com.atomist.tree.content.text.grammar.antlr.AntlrGrammar
import com.atomist.tree.content.text.grammar.{MatchListener, Parser}
import com.atomist.tree.content.text.MutableContainerTreeNode
import com.atomist.tree.content.text.TreeNodeOperations._
import com.atomist.util.Utils.withCloseable
import org.apache.commons.io.IOUtils
import org.springframework.core.io.DefaultResourceLoader

/**
  * Python3 parser. Uses Antlr.
  */
class Python3Parser extends Parser {

  import Python3Parser._

  val cp = new DefaultResourceLoader()

  val r = cp.getResource("classpath:grammars/antlr/Python3.g4")

  val g4 = withCloseable(r.getInputStream)(is => IOUtils.toString(is, StandardCharsets.UTF_8))

  private lazy val pythonGrammar = new AntlrGrammar(g4, "file_input")

  private val removeNewlines: TreeOperation =
    removeProductionsNamed(Set("NEWLINE"))

  private val stripPythonReservedWords: TreeOperation =
    removeReservedWordTokens(PythonReservedWords)

  override def parse(input: String, ml: Option[MatchListener] = None): MutableContainerTreeNode = {
    val raw = pythonGrammar.parse(input, ml)
    val ast = (stripPythonReservedWords andThen removeNewlines andThen RemovePadding andThen Prune)(raw)
    ast
  }
}

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