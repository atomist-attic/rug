package com.atomist.rug.kind.grammar

import java.nio.charset.StandardCharsets

import com.atomist.project.ProjectOperationArguments
import com.atomist.rug.kind.core.{FileArtifactBackedMutableView, FileMutableView, ProjectMutableView}
import com.atomist.rug.kind.dynamic.{ContextlessViewFinder, MutableContainerMutableView, MutableTreeNodeUpdater}
import com.atomist.rug.parser.Selected
import com.atomist.rug.runtime.rugdsl.{DefaultEvaluator, Evaluator}
import com.atomist.rug.spi.{MutableView, ReflectivelyTypedType, Type}
import com.atomist.source.{ArtifactSource, FileArtifact}
import com.atomist.tree.TreeNode
import com.atomist.tree.content.text.MutableContainerTreeNode
import com.atomist.tree.content.text.grammar.antlr.AntlrGrammar
import com.atomist.util.Utils.withCloseable
import org.apache.commons.io.IOUtils
import org.springframework.core.io.DefaultResourceLoader

import scala.collection.JavaConverters._

/**
  * Convenient superclass for Antlr grammars.
  *
  * @param grammars           g4 files
  * @param topLevelProduction name of the top level production
  */
abstract class AntlrRawFileType(
                                 topLevelProduction: String,
                                 grammars: String*
                               )
  extends Type(DefaultEvaluator)
    with ReflectivelyTypedType
    with ContextlessViewFinder {

  private val g4s: Seq[String] = {
    val cp = new DefaultResourceLoader()
    val resources = grammars.map(grammar => cp.getResource(grammar))
    resources.map(r => withCloseable(r.getInputStream)(is => IOUtils.toString(is, StandardCharsets.UTF_8)))
  }

  private lazy val antlrGrammar = new AntlrGrammar(topLevelProduction, g4s: _*)

  final override def resolvesFromNodeTypes = Set("Project", "File")

  protected def isOfType(f: FileArtifactBackedMutableView): Boolean

  override def viewManifest: Manifest[MutableContainerMutableView] = manifest[MutableContainerMutableView]

  override protected def findAllIn(rugAs: ArtifactSource,
                                   selected: Selected,
                                   context: TreeNode,
                                   poa: ProjectOperationArguments,
                                   identifierMap: Map[String, Object]): Option[Seq[TreeNode]] = {
    context match {
      case pmv: ProjectMutableView =>
        Some(pmv
          .files
          .asScala
          .filter(isOfType)
          .map(f => toView(f))
        )
      case f: FileMutableView if isOfType(f) =>
        Some(Seq(toView(f)))
      case _ => None
    }
  }

  private def toView(f: FileArtifactBackedMutableView): MutableView[_] = {
    val rawNode = parseToRawNode(f.content)
    val mtn = new MutableContainerMutableView(rawNode, f)
    // Ensure the file is updated based on any changes to the underlying AST at any level
    f.registerUpdater(new MutableTreeNodeUpdater(mtn.currentBackingObject))
    mtn
  }

  /**
    * Return a parsed node. Useful to validate content, for example in tests.
    * @param content content to parse
    * @return
    */
  def parseToRawNode(content: String): MutableContainerTreeNode = {
    antlrGrammar.parse(content, None)
  }
}
