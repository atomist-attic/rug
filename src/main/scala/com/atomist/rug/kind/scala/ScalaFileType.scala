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

  override def fileToRawNode(f: FileArtifact, ml: Option[MatchListener]): Option[MutableContainerTreeNode] = {
    f.content.parse[Source] match {
      case Success(ast) =>
        val smTree = new ScalaMetaTreeBackedTreeNode(ast)
        val returnedTree = SimpleMutableContainerTreeNode.makeMutable(smTree, f.content)
        //println(TreeNodeUtils.toShorterString(returnedTree))
        Some(returnedTree)
      case _ => None
    }
  }

}

