package com.atomist.rug.kind.csharp

import com.atomist.rug.kind.core.FileArtifactBackedMutableView
import com.atomist.rug.kind.grammar.AntlrRawFileType

object CSharpFileType {

  val CSharpExtension = ".cs"
}

class CSharpFileType
  extends AntlrRawFileType(topLevelProduction = "compilation_unit",
    "classpath:grammars/antlr/CSharpLexer.g4",
    "classpath:grammars/antlr/CSharpParser.g4"
  ) {

  import CSharpFileType._

  override def description = "C# file"

  override protected def isOfType(f: FileArtifactBackedMutableView): Boolean =
    f.filename.endsWith(CSharpExtension)

}
