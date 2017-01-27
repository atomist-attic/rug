package com.atomist.rug.kind.grammar

import com.atomist.source.ArtifactSource
import org.scalatest.{FlatSpec, Matchers}

/**
  * Convenient superclass for Antlr types
  */
abstract class AntlrRawFileTypeTest extends FlatSpec with Matchers {

  protected def typeBeingTested: AntlrRawFileType

  /**
    * Validate all files of the type we're interested in in the given result
    * @param r result artifactsource
    */
  protected def validateResultContainsValidFiles(r: ArtifactSource): Unit = {
    val goodFileCount = r.allFiles
      .filter(typeBeingTested.isOfType)
      .map(cs => (cs, typeBeingTested.parseToRawNode(cs.content)))
      .map(tup => tup._2.getOrElse(fail(s"Cannot parse file\n${tup._1.content}")))
      .count(tree => tree.childNodes.nonEmpty)
    goodFileCount should be >=(1)
  }
}
