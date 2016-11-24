package com.atomist.rug.kind.json

import java.nio.charset.StandardCharsets

import com.atomist.model.content.grammar.antlr.AntlrGrammar
import com.atomist.model.content.grammar.{MatchListener, Parser}
import com.atomist.model.content.text.MutableContainerTreeNode
import com.atomist.model.content.text.TreeNodeOperations._
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

  private lazy val jsGrammar = new AntlrGrammar(g4, "json")

  override def parse(input: String, ml: Option[MatchListener] = None): MutableContainerTreeNode = {
    val raw = jsGrammar.parse(input, ml)
    val r = (RemovePadding andThen Prune)(raw)
    println(r)
    r
  }
}
