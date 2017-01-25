package com.atomist.rug.kind.grammar

import java.nio.charset.StandardCharsets

import com.atomist.project.ProjectOperationArguments
import com.atomist.rug.kind.core.{FileMutableView, ProjectMutableView}
import com.atomist.rug.kind.dynamic.{ContextlessViewFinder, MutableContainerMutableView}
import com.atomist.rug.parser.Selected
import com.atomist.rug.runtime.rugdsl.{DefaultEvaluator, Evaluator}
import com.atomist.rug.spi.{ReflectivelyTypedType, Type}
import com.atomist.source.{ArtifactSource, FileArtifact}
import com.atomist.tree.TreeNode
import com.atomist.tree.content.text.grammar.antlr.AntlrGrammar
import com.atomist.util.Utils.withCloseable
import org.apache.commons.io.IOUtils
import org.springframework.core.io.DefaultResourceLoader

/**
  * Convenient superclass for Antlr grammars.
  * @param evaluator used to evaluate expressions
  * @param grammar g4 file
  */
abstract class AntlrRawFileType(
                         evaluator: Evaluator,
                         grammar: String
                       )
  extends Type(evaluator)
    with ReflectivelyTypedType
    with ContextlessViewFinder {

  def this(grammar: String) = this(DefaultEvaluator, grammar)

  private val cp = new DefaultResourceLoader()

  private val r = cp.getResource(grammar)

  private val g4 = withCloseable(r.getInputStream)(is => IOUtils.toString(is, StandardCharsets.UTF_8))

  private lazy val antlrGrammar = new AntlrGrammar(g4, "file_input")

  final override def resolvesFromNodeTypes = Set("Project", "File")

  protected def isOfType(f: FileArtifact): Boolean

  override def viewManifest: Manifest[MutableContainerMutableView] = manifest[MutableContainerMutableView]

  override protected def findAllIn(rugAs: ArtifactSource,
                                   selected: Selected,
                                   context: TreeNode,
                                   poa: ProjectOperationArguments,
                                   identifierMap: Map[String, Object]): Option[Seq[TreeNode]] = {
    context match {
      case pmv: ProjectMutableView =>
        Some(pmv.currentBackingObject
          .files
          .filter(isOfType)
          .map(f => toView(f, pmv))
        )
      case f: FileMutableView if isOfType(f.currentBackingObject) =>
        Some(Seq(toView(f.currentBackingObject, f.parent)))
      case _ => None
    }
  }

  private def toView(f: FileArtifact, pmv: ProjectMutableView): TreeNode = {
    antlrGrammar.parse(f.content, None)
  }
}
