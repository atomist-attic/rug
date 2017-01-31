package com.atomist.rug.ts

import _root_.java.io.File

import com.atomist.project.SimpleProjectOperationArguments
import com.atomist.rug.compiler.typescript.TypeScriptCompiler
import com.atomist.source.ArtifactSource
import com.atomist.source.file.{FileSystemArtifactSource, FileSystemArtifactSourceIdentifier}
import com.atomist.source.filter.ArtifactFilter

/**
  * Helps us compile TypeScript archives.
  * in a node_modules directory that was copied there by a maven goal called copy-ts, which takes it from src/main/typescript
  */
object TypeScriptBuilder {

  val compiler = new TypeScriptCompiler()

  val userModel: ArtifactSource = {
    val generator = new TypeScriptInterfaceGenerator
    val output = generator.generate("stuff", SimpleProjectOperationArguments("", Map(generator.OutputPathParam -> "Core.ts")))
    val src = new FileSystemArtifactSource(FileSystemArtifactSourceIdentifier(new File("src/main/typescript")), new ArtifactFilter {
      override def apply(s: String) = {!s.endsWith(".js")}
    }) // THIS ONLY WORKS IN TESTS NOT IN PRODUCTION BY DESIGN
    val compiled = compiler.compile(src.underPath("node_modules/@atomist").withPathAbove(".atomist") + output.withPathAbove(".atomist/rug/model"))
    compiled.underPath(".atomist").withPathAbove(".atomist/node_modules/@atomist")
  }

  /**
    * Compile the given archive contents along with our generated TypeScript model
    */
  def compileWithModel(tsAs: ArtifactSource): ArtifactSource = {
    compiler.compile(userModel + tsAs)
  }

}
