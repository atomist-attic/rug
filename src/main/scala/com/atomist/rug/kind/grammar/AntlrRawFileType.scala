package com.atomist.rug.kind.grammar

import java.nio.charset.StandardCharsets

import com.atomist.source.FileArtifact
import com.atomist.tree.content.text.PositionedTreeNode
import com.atomist.tree.content.text.grammar.antlr.{AntlrGrammar, AstNodeCreationStrategy}
import com.atomist.util.Utils.withCloseable
import org.apache.commons.io.IOUtils
import org.springframework.core.io.DefaultResourceLoader

/**
  * Convenient superclass for Antlr grammars.
  *
  * @param grammars           g4 files
  * @param topLevelProduction name of the top level production
  */
abstract class AntlrRawFileType(
                                 topLevelProduction: String,
                                 nodeCreationStrategy: AstNodeCreationStrategy,
                                 grammars: String*
                               )
   extends TypeUnderFile {

  private val g4s: Seq[String] = {
    val cp = new DefaultResourceLoader()
    val resources = grammars.map(grammar => cp.getResource(grammar))
    resources.map(r => withCloseable(r.getInputStream)(is => IOUtils.toString(is, StandardCharsets.UTF_8)))
  }

  private[kind] def parser = antlrGrammar

  private lazy val antlrGrammar = new AntlrGrammar(topLevelProduction, nodeCreationStrategy, g4s: _*)

  override def fileToRawNode(f: FileArtifact): Option[PositionedTreeNode] = {
    antlrGrammar.parse(f.content)
  }
}
