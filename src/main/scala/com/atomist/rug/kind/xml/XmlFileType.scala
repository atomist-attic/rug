package com.atomist.rug.kind.xml

import com.atomist.rug.kind.core.FileArtifactBackedMutableView
import com.atomist.rug.kind.dynamic.MutableContainerMutableView
import com.atomist.rug.kind.grammar.AntlrRawFileType
import com.atomist.source.FileArtifact
import com.atomist.tree.TreeNode
import com.atomist.tree.TreeNode.Significance
import com.atomist.tree.content.text.{MutableContainerTreeNode, TreeNodeOperations}
import com.atomist.tree.content.text.grammar.antlr.{AstNodeNamingStrategy, FromGrammarNamingStrategy}

object XmlFileType {

  val XmlExtension = ".xml"

}

class XmlFileType
  extends AntlrRawFileType("document",
    XmlNamingStrategy,
    "classpath:grammars/antlr/XMLParser.g4",
    "classpath:grammars/antlr/XMLLexer.g4"
  ) {

  import XmlFileType._

  override def description = "XML file"

  override def isOfType(f: FileArtifact): Boolean =
    f.name.endsWith(XmlExtension)

  override protected def createView(n: MutableContainerTreeNode, f: FileArtifactBackedMutableView): MutableContainerMutableView = {
    super.createView(TreeNodeOperations.Flatten(n), f)
  }
}


private object XmlNamingStrategy extends AstNodeNamingStrategy {

  override def nameForContainer(rule: String, fields: Seq[TreeNode]): String = rule match {
    case "element" =>
      fields.find(f => f.nodeName == "Name") match {
        case Some(nameField) => nameField.value
        case None => "element"
      }

    case x => x
  }

  override def significance(rule: String, fields: Seq[TreeNode]): Significance = rule match {
    case "content" => TreeNode.Noise
    case _ => TreeNode.Undeclared
  }
}