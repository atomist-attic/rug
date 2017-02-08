package com.atomist.rug.kind.grammar

import com.atomist.project.SimpleProjectOperationArguments
import com.atomist.project.edit.{ModificationAttempt, SuccessfulModification}
import com.atomist.rug.TestUtils
import com.atomist.source.ArtifactSource
import com.atomist.tree.pathexpression.{ExpressionEngine, PathExpressionEngine}
import org.scalatest.{FlatSpec, Matchers}

/**
  * Convenient superclass for types extending TypeUnderFile. Offers convenience
  * methods for modification and reparses files using the underlying
  * type to ensure any modifications leave them well-formed.
  */
abstract class AbstractTypeUnderFileTest extends FlatSpec with Matchers {

  protected def typeBeingTested: TypeUnderFile

  /** For use by subclasses */
  protected val expressionEngine: ExpressionEngine = new PathExpressionEngine

  /**
    * Validate all files of the type we're interested in in the given result
    * @param r result artifactsource
    */
  protected def validateResultContainsValidFiles(r: ArtifactSource): Unit = {
    val goodFileCount = r.allFiles
      .filter(typeBeingTested.isOfType)
      .map(cs => (cs, typeBeingTested.fileToRawNode(cs)))
      .map(tup => tup._2.getOrElse(fail(s"Cannot parse file\n[${tup._1.content}]")))
      .count(tree => tree.childNodes.nonEmpty)
    goodFileCount should be >=(1)
  }

  protected def modify(tsFilename: String, as: ArtifactSource, params: Map[String, String] = Map()): ModificationAttempt = {
    val pe = TestUtils.editorInSideFile(this, tsFilename)
    pe.modify(as, SimpleProjectOperationArguments("", params))
  }

  protected def modifyAndReparseSuccessfully(tsFilename: String, as: ArtifactSource, params: Map[String, String] = Map()): ArtifactSource = {
    modify(tsFilename, as, params) match {
      case sm: SuccessfulModification =>
        validateResultContainsValidFiles(sm.result)
        sm.result
      case _ => ???
    }
  }
}
