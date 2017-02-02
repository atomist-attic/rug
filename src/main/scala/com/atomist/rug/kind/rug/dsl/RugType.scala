package com.atomist.rug.kind.rug.dsl

import com.atomist.project.ProjectOperationArguments
import com.atomist.rug.kind.core.{DirectoryMutableView, FileArtifactBackedMutableView, ProjectMutableView}
import com.atomist.rug.kind.grammar.AntlrRawFileType
import com.atomist.rug.parser.Selected
import com.atomist.rug.runtime.rugdsl.Evaluator
import com.atomist.rug.spi.{MutableView, ReflectivelyTypedType, Type}
import com.atomist.source.{ArtifactSource, FileArtifact}
import com.atomist.tree.TreeNode
import com.atomist.tree.content.text.grammar.antlr.FromGrammarNamingStrategy

class EditorType(evaluator: Evaluator) extends Type(evaluator) with ReflectivelyTypedType {
  /** Describe the MutableView subclass to allow for reflective function export */

  override def viewManifest: Manifest[_] = manifest[EditorMutableView]

  override protected def findAllIn(rugAs: ArtifactSource,
                                   selected: Selected,
                                   context: TreeNode,
                                   poa: ProjectOperationArguments,
                                   identifierMap: Map[String, Object]): Option[Seq[MutableView[_]]] =
    context match {
      case pmv: ProjectMutableView => ???
      case f: FileArtifactBackedMutableView =>
        Some(Seq(new EditorMutableView(f.currentBackingObject, f.parent)))
      case d: DirectoryMutableView => ???
      case _ => None
    }

  /**
    * Description of this type
    */
  override def description: String = "Rug Editor, in Rug DSL or typescript"

}

class RugFileType
  extends AntlrRawFileType(
    topLevelProduction = "rug_file",
    namingStrategy = FromGrammarNamingStrategy,
    grammars = "classpath:grammars/antlr/Rug.g4") {

  override def description = "Rug DSL file"

  override def isOfType(f: FileArtifact): Boolean =
    f.name.endsWith(".rug")

}
