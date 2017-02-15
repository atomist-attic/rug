package com.atomist.rug.kind.scala

import com.atomist.rug.kind.grammar.TypeUnderFile
import com.atomist.source.FileArtifact
import com.atomist.tree.content.text.grammar.MatchListener
import com.atomist.tree.content.text.{MutableContainerTreeNode, PositionedTreeNode, SimpleMutableContainerTreeNode}
import com.atomist.tree.utils.TreeNodeUtils

import scala.meta._
import scala.meta.parsers.Parsed.Success

class ScalaFileType extends TypeUnderFile {

  override def description: String = "Scala file"

  override def isOfType(f: FileArtifact): Boolean = f.name.endsWith(".scala")

  override def fileToRawNode(f: FileArtifact, ml: Option[MatchListener]): Option[PositionedTreeNode] = {
    f.content.parse[Source] match {
      case Success(ast) =>
        val ourTree = new ScalaMetaTreeBackedTreeNode(ast)
        Some(ourTree)
      case other =>
        //println(s"Failure to parse Scala Meta: $other")
        None
    }
  }
}