package com.atomist.rug.kind.javascript

import java.nio.charset.StandardCharsets

import com.atomist.tree.content.grammar.antlr.AntlrGrammar
import com.atomist.tree.content.grammar.{MatchListener, Parser}
import com.atomist.tree.content.text.MutableContainerTreeNode
import com.atomist.tree.utils.TreeNodeOperations._
import com.atomist.util.Utils.withCloseable
import org.apache.commons.io.IOUtils
import org.springframework.core.io.DefaultResourceLoader

/**
  * JavaScript parser. Uses Antlr.
  */
class JavaScriptParser extends Parser {

  val cp = new DefaultResourceLoader()

  val r = cp.getResource("classpath:grammars/antlr/ECMAScript.g4")

  val g4 = withCloseable(r.getInputStream)(is => IOUtils.toString(is, StandardCharsets.UTF_8))

  private lazy val jsGrammar = new AntlrGrammar(g4, "program")

  override def parse(input: String, ml: Option[MatchListener] = None): MutableContainerTreeNode = {
    jsGrammar.parse(input, ml)
  }
}
