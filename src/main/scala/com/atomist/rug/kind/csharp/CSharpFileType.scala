package com.atomist.rug.kind.csharp

import com.atomist.rug.kind.core.FileArtifactBackedMutableView
import com.atomist.rug.kind.dynamic.MutableContainerMutableView
import com.atomist.rug.kind.grammar.{AntlrRawFileType, RawNodeUnderFileMutableView}
import com.atomist.rug.spi.{ExportFunction, ExportFunctionParameterDescription}
import com.atomist.source.FileArtifact
import com.atomist.tree.content.text.MutableContainerTreeNode
import com.atomist.tree.content.text.grammar.antlr.FromGrammarAstNodeCreationStrategy
import com.atomist.tree.pathexpression.{PathExpression, PathExpressionParser}

object CSharpFileType {

  val CSharpExtension = ".cs"
}

class CSharpFileType
  extends AntlrRawFileType(topLevelProduction = "compilation_unit",
    FromGrammarAstNodeCreationStrategy,
    "classpath:grammars/antlr/CSharpLexer.g4",
    "classpath:grammars/antlr/CSharpParser.g4"
  ) {

  import CSharpFileType._

  override def viewManifest: Manifest[CSharpFileMutableView] =
    manifest[CSharpFileMutableView]

  override def description = "C# file"

  override def isOfType(f: FileArtifact): Boolean =
    f.name.endsWith(CSharpExtension)

  override protected def createView(n: MutableContainerTreeNode, f: FileArtifactBackedMutableView): MutableContainerMutableView = {
    // Create a special view
    new CSharpFileMutableView(n, f)
  }

}


object CSharpFileMutableView {

  import PathExpressionParser.parseString

  val FirstUsingStatement: PathExpression = "//using_directive[1]"

}

/**
  * Special type to hold top level methods on CSharp files
  */
class CSharpFileMutableView(topLevelNode: MutableContainerTreeNode, f: FileArtifactBackedMutableView)
  extends RawNodeUnderFileMutableView(topLevelNode, f) {

  import CSharpFileMutableView._

  @ExportFunction(readOnly = false, description = "Add a using if it isn't already present")
  def addUsing(@ExportFunctionParameterDescription(
    name = "newUsing",
    description = "New using (just the package)")
               newUsing: String): Unit = {
    val newUsingStatement = s"using $newUsing;"
    if (!f.content.contains(newUsingStatement))
      doWithNodesMatchingPath(FirstUsingStatement, mtn =>
        mtn.update(s"${mtn.value}\n$newUsingStatement\n")
      )
  }

}



