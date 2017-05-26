package com.atomist.rug.kind.xml

import com.atomist.rug.kind.grammar.AntlrRawFileType
import com.atomist.source.FileArtifact
import com.atomist.tree.TreeNode
import com.atomist.tree.TreeNode.Significance
import com.atomist.tree.content.text.grammar.antlr.{AntlrPositionedTreeNode, AstNodeCreationStrategy}

object XmlFileType {

  val XmlExtension = ".xml"
}

class XmlFileType
  extends AntlrRawFileType("document",
    XmlAstNodeCreationStrategy,
    "classpath:grammars/antlr/XMLParser.g4",
    "classpath:grammars/antlr/XMLLexer.g4"
  ) {

  import XmlFileType._

  override def description = "XML file"

  override def isOfType(f: FileArtifact): Boolean =
    f.name.endsWith(XmlExtension)

}

private object XmlAstNodeCreationStrategy extends AstNodeCreationStrategy {

  override def nameForContainer(rule: String, fields: Seq[AntlrPositionedTreeNode]): String = rule match {
    case "element" =>
      fields.find(f => f.nodeName == "Name") match {
        case Some(nameField) => nameField.value
        case None => "element"
      }
    case x => x
  }

  override def significance(rule: String, fields: Seq[AntlrPositionedTreeNode]): Significance = rule match {
    case "content" =>
      // We want to flatten this layer
      TreeNode.Noise
    case _ =>
      TreeNode.Signal
  }
}