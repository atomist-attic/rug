package com.atomist.rug.rugdoc

import com.atomist.param.SimpleParameterValues
import com.atomist.rug.ts.{AbstractTypeScriptGenerator, TypeScriptInterfaceGenerator}

/**
  * Use this to actually generate interfaces. Of course, we
  * should ultimately use the interface generator as an editor.
  */
object TypeScriptInterfaceGen extends App {

  val td = new TypeScriptInterfaceGenerator()
  // Make it puts the generated files where our compiler will look for them
  val output = td.generate("", SimpleParameterValues(
    Map(AbstractTypeScriptGenerator.OutputPathParam -> ".atomist/editors/Interfaces.ts")))
  val d = output.allFiles.head
  // println(d.content)
}
