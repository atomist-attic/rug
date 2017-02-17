package com.atomist.rug.kind.grammar

import com.atomist.graph.GraphNode
import com.atomist.param.SimpleParameterValues
import com.atomist.project.edit.{ModificationAttempt, SuccessfulModification}
import com.atomist.rug.TestUtils
import com.atomist.rug.kind.DefaultTypeRegistry
import com.atomist.rug.kind.core.ProjectMutableView
import com.atomist.source.{ArtifactSource, FileArtifact, SimpleFileBasedArtifactSource}
import com.atomist.tree.TreeNode
import com.atomist.tree.content.text.{OverwritableTextTreeNode, TextTreeNodeLifecycle}
import com.atomist.tree.content.text.microgrammar.MatcherMicrogrammar
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
    *
    * @param r result artifactsource
    */
  protected def validateResultContainsValidFiles(r: ArtifactSource): Unit = {
    val filesOfType = r.allFiles.filter(typeBeingTested.isOfType)
    filesOfType.size should be >= (1)
    withClue(s"files named ${filesOfType.map(_.path).mkString(",")}") {
      val parsedFiles = filesOfType.map(cs => (cs, typeBeingTested.fileToRawNode(cs)))
        .map(tup => tup._2.getOrElse(fail(s"Cannot parse file\n[${tup._1.content}]")))
      val goodFileCount = parsedFiles.count(tree => tree.childNodes.nonEmpty)
      goodFileCount should be >= (1)
    }
  }

  /**
    * Require successful evaluation of this expression against the given root node
    */
  protected def evaluatePathExpression(tn: GraphNode, pe: String): Seq[GraphNode] = {
    import com.atomist.tree.pathexpression.PathExpressionParser._
    expressionEngine.evaluate(tn, pe, DefaultTypeRegistry) match {
      case Right(nodes) => nodes
      case Left(x) => fail(s"Path expression failure: $x executing [$pe]")
    }
  }

  protected def parseAndPad(file: FileArtifact): String = {
    val as = SimpleFileBasedArtifactSource(file)
    val pmv = new ProjectMutableView(as, as)
    val fmv = pmv.files.get(0)
    val parsed = typeBeingTested.fileToRawNode(file).get
    val nodes = TextTreeNodeLifecycle.makeReady(typeBeingTested.name, Seq(parsed), fmv)
    nodes.head.update(nodes.head.value) // trigger an update to file contents
    fmv.content
  }

  protected def modify(tsFilename: String, as: ArtifactSource, params: Map[String, String] = Map()): ModificationAttempt = {
    val pe = TestUtils.editorInSideFile(this, tsFilename)
    pe.modify(as, SimpleParameterValues(params))
  }

  protected def modifyAndReparseSuccessfully(tsFilename: String, as: ArtifactSource, params: Map[String, String] = Map()): ArtifactSource = {
    modify(tsFilename, as, params) match {
      case sm: SuccessfulModification =>
        validateResultContainsValidFiles(sm.result)
        sm.result
      case boohoo => fail(s"It did not successfully modify: $boohoo")
    }
  }
}
