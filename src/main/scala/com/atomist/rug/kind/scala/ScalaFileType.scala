package com.atomist.rug.kind.scala

import com.atomist.rug.kind.grammar.TypeUnderFile
import com.atomist.source.FileArtifact
import com.atomist.tree.content.text.MutableContainerTreeNode
import com.atomist.tree.content.text.grammar.MatchListener

import scala.meta._
import scala.meta.parsers.Parsed.Success

class ScalaFileType extends TypeUnderFile {

  override def description: String = "Scala file"

  override def isOfType(f: FileArtifact): Boolean = f.name.endsWith(".scala")

  override def contentToRawNode(content: String, ml: Option[MatchListener]): Option[MutableContainerTreeNode] = {
    content.parse[Source] match {
      case Success(ast) => Some(new ScalaMetaTreeBackedMutableTreeNode(ast))
      case _ => None
    }
  }

}

