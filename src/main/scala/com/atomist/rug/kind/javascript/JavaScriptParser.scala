package com.atomist.rug.kind.javascript

import java.nio.charset.StandardCharsets

import com.atomist.tree.content.text.grammar.antlr.{AntlrGrammar, FromGrammarNamingStrategy}
import com.atomist.tree.content.text.grammar.{MatchListener, Parser}
import com.atomist.tree.content.text.MutableContainerTreeNode
import com.atomist.tree.content.text.TreeNodeOperations._
import com.atomist.util.Utils.withCloseable
import org.apache.commons.io.IOUtils
import org.springframework.core.io.DefaultResourceLoader

/**
  * JavaScript parser. Uses Antlr.
  */
class JavaScriptParser extends Parser {

  val g4: String = {
    val cp = new DefaultResourceLoader()
    val r = cp.getResource("classpath:grammars/antlr/ECMAScript.g4")
    withCloseable(r.getInputStream)(is => IOUtils.toString(is, StandardCharsets.UTF_8))
  }

  private lazy val jsGrammar = new AntlrGrammar("program", FromGrammarNamingStrategy, g4)

  override def parse(input: String, ml: Option[MatchListener] = None): Option[MutableContainerTreeNode] = {
    jsGrammar.parse(input, ml)
  }
}
