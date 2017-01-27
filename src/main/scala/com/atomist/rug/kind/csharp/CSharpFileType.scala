package com.atomist.rug.kind.csharp

import com.atomist.rug.kind.core.FileArtifactBackedMutableView
import com.atomist.rug.kind.dynamic.MutableContainerMutableView
import com.atomist.rug.kind.grammar.AntlrRawFileType
import com.atomist.rug.spi.ExportFunction
import com.atomist.source.FileArtifact
import com.atomist.tree.content.text.MutableContainerTreeNode

object CSharpFileType {

  val CSharpExtension = ".cs"
}

class CSharpFileType
  extends AntlrRawFileType(topLevelProduction = "compilation_unit",
    "classpath:grammars/antlr/CSharpLexer.g4",
    "classpath:grammars/antlr/CSharpParser.g4"
  ) {

  import CSharpFileType._

  override def viewManifest: Manifest[CSharpFileMutableView] =
    manifest[CSharpFileMutableView]

  override def description = "C# file"

  override def isOfType(f: FileArtifact): Boolean =
    f.name.endsWith(CSharpExtension)

  /**
    * Subclasses can override this if they want to customize the top level node created:
    * for example, to add verbs that can be used instead of drilling into path expressions.
    *
    * @return new mutable view
    */
  override protected def createView(n: MutableContainerTreeNode, f: FileArtifactBackedMutableView): MutableContainerMutableView = {
    new CSharpFileMutableView(n, f)
  }

}


/**
  * Special type to hold top level methods on CSharp files
  * @param n
  * @param f
  */
class CSharpFileMutableView(n: MutableContainerTreeNode, f: FileArtifactBackedMutableView)
  extends MutableContainerMutableView(n, f) {

  override def nodeType: Set[String] = super.nodeType ++ Set("CSharpFile")

  @ExportFunction(readOnly = false, description = "Add a using")
  def addUsing(newUsing: String): Unit = {
    ???
  }

}
