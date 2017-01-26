package com.atomist.rug.kind.json

import java.nio.charset.StandardCharsets

import com.atomist.tree.content.text.grammar.antlr.AntlrGrammar
import com.atomist.tree.content.text.grammar.{MatchListener, Parser}
import com.atomist.tree.content.text.MutableContainerTreeNode
import com.atomist.tree.content.text.TreeNodeOperations._
import com.atomist.util.Utils.withCloseable
import org.apache.commons.io.IOUtils
import org.springframework.core.io.DefaultResourceLoader

/**
  * JavaScript parser. Uses Antlr.
  */
class JsonParser extends Parser {

  val cp = new DefaultResourceLoader()

  val r = cp.getResource("classpath:grammars/antlr/JSON.g4")

  val g4 = withCloseable(r.getInputStream)(is => IOUtils.toString(is, StandardCharsets.UTF_8))

  private lazy val jsGrammar = new AntlrGrammar("json", g4)

  override def parse(input: String, ml: Option[MatchListener] = None): MutableContainerTreeNode = {
    val raw = jsGrammar.parse(input, ml)
    val r = (RemovePadding andThen Prune)(raw)
    r
  }
}
