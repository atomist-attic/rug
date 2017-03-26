package com.atomist.rug.ts

import _root_.java.io.File

import com.atomist.param.SimpleParameterValues
import com.atomist.rug.compiler.typescript.TypeScriptCompiler
import com.atomist.rug.compiler.typescript.compilation.CompilerFactory
import com.atomist.source.{ArtifactSource, FileArtifact, FileEditor}
import com.atomist.source.file.{FileSystemArtifactSource, FileSystemArtifactSourceIdentifier}
import com.atomist.source.filter.ArtifactFilter

/**
  * Helps us compile TypeScript archives
  * in a node_modules directory that was copied there by a
  * Maven goal called copy-ts, which takes it from src/main/typescript.
  */
object TypeScriptBuilder {

  import CortexTypeGenerator._

  val compiler = new TypeScriptCompiler(CompilerFactory.cachingCompiler(CompilerFactory.create(),
    new File(".", "target" + File.separator + ".jscache").getAbsolutePath))

  // We need to use relative imports in tests
  private val testTimeUserModel = new FileEditor {
    override def canAffect(f: FileArtifact) = true

    override def edit(f: FileArtifact): FileArtifact = {
      // TODO fragile, depends on hard-coded stub to decide on depth to back up
      val extra = if (f.path.contains("stub")) "../" else ""
      val newF = f.withContent(
        f.content
          .replace("@atomist/rug/tree", s"$extra../tree")
          .replace("@atomist/rug/operations", s"$extra../operations")
      )
      newF
    }
  }

  def compileUserModel(rawSources: Seq[ArtifactSource]): ArtifactSource = {
    val sources = rawSources.map(_.edit(testTimeUserModel))
    val src = new FileSystemArtifactSource(FileSystemArtifactSourceIdentifier(
      new File("src/main/typescript")), new ArtifactFilter {
      override def apply(s: String) =
        !s.endsWith(".js")
    })
    // THIS ONLY WORKS IN TESTS NOT IN PRODUCTION BY DESIGN
    val compiled = compiler.compile(src.underPath("node_modules/@atomist").withPathAbove(".atomist")
      + sources.reduce((a, b) => a + b))
    compiled.underPath(".atomist").withPathAbove(".atomist/node_modules/@atomist")
  }

  val coreSource: ArtifactSource = {
    val generator = new TypeScriptInterfaceGenerator
    generator.generate("stuff", SimpleParameterValues(Map(generator.outputPathParam -> "Core.ts")))
      .withPathAbove(".atomist/rug/model")
  }

  val extendedModelSource: ArtifactSource = {
    val tg = new CortexTypeGenerator(DefaultCortexDir, DefaultCortexStubDir)
    tg.toNodeModule(DefaultTypeGeneratorConfig.CortexJson)
      .withPathAbove(".atomist/rug")
  }

  val userModel: ArtifactSource = compileUserModel(Seq(coreSource))

  val extendedModel: ArtifactSource = compileUserModel(Seq(coreSource, extendedModelSource))

  /**
    * Compile the given archive contents along with our generated TypeScript model
    */
  def compileWithModel(tsAs: ArtifactSource): ArtifactSource =
    compiler.compile(userModel + tsAs)

  def compileWithExtendedModel(tsAs: ArtifactSource): ArtifactSource =
    compiler.compile(userModel + extendedModel + tsAs)

}
