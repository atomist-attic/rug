package com.atomist.rug.kind.python3

import com.atomist.rug.kind.grammar.AntlrRawFileType
import com.atomist.rug.kind.python3.PythonFileType._
import com.atomist.source.FileArtifact

class PythonRawFileType
  extends AntlrRawFileType("classpath:grammars/antlr/Python3.g4") {

  override def description = "Python file"

  override protected def isOfType(f: FileArtifact): Boolean =
    f.name.endsWith(PythonExtension)

}
