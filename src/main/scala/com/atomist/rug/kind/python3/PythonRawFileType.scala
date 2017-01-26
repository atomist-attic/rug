package com.atomist.rug.kind.python3

import com.atomist.rug.kind.core.FileArtifactBackedMutableView
import com.atomist.rug.kind.grammar.AntlrRawFileType
import com.atomist.rug.kind.python3.PythonFileType._

class PythonRawFileType
  extends AntlrRawFileType("file_input", "classpath:grammars/antlr/Python3.g4") {

  override def description = "Python file"

  override protected def isOfType(f: FileArtifactBackedMutableView): Boolean =
    f.filename.endsWith(PythonExtension)

}
