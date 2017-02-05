package com.atomist.rug.kind.scala

import com.atomist.rug.kind.grammar.TypeUnderFile
import com.atomist.source.FileArtifact
import com.atomist.tree.content.text.grammar.MatchListener
import com.atomist.tree.content.text.{MutableContainerTreeNode, SimpleMutableContainerTreeNode}

import scala.meta._
import scala.meta.parsers.Parsed.Success

class ScalaFileType extends TypeUnderFile {

  override def description: String = "Scala file"

  override def isOfType(f: FileArtifact): Boolean = f.name.endsWith(".scala")

  override def contentToRawNode(content: String, ml: Option[MatchListener]): Option[MutableContainerTreeNode] = {
    content.parse[Source] match {
      case Success(ast) =>
        val smTree = new ScalaMetaTreeBackedTreeNode(ast)
        val returnedTree = SimpleMutableContainerTreeNode.makeMutable(smTree, content)
        //println(TreeNodeUtils.toShorterString(returnedTree))
        Some(returnedTree)
      case _ => None
    }
  }

}

