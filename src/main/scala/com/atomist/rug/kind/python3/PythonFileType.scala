package com.atomist.rug.kind.python3

import com.atomist.rug.kind.grammar.AntlrRawFileType
import com.atomist.source.FileArtifact

object PythonFileType {

  val PythonExtension = ".py"

}

class PythonFileType
  extends AntlrRawFileType("file_input", "classpath:grammars/antlr/Python3.g4") {

  import PythonFileType._

  override def description = "Python file"

  override def isOfType(f: FileArtifact): Boolean =
    f.name.endsWith(PythonExtension)

}
