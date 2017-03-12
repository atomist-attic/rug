package com.atomist.rug.rugdoc

import com.atomist.param.SimpleParameterValues
import com.atomist.rug.ts.{InterfaceGenerationConfig, TypeScriptBuilder, TypeScriptInterfaceGenerator}
import com.atomist.source.{ArtifactSource, FileArtifact, FileEditor}
import org.scalatest.{FlatSpec, Matchers}

object TypeScriptInterfaceGeneratorTest {

  val tsc = TypeScriptBuilder.compiler

  /**
    * We need to get rid of the imports as they'll fail
    * when we try to compile the file on its own
    */
  private def withoutImports(output: ArtifactSource): ArtifactSource =
    output ✎ new FileEditor {
      override def canAffect(f: FileArtifact): Boolean = true

      // Note: We need to pretend we have imports that will be available
      // at runtime
      override def edit(f: FileArtifact): FileArtifact =
      f.withContent(f.content.replace(InterfaceGenerationConfig().imports,
        """
          |interface ProjectContext {}
          |interface PathExpressionEngine {}
          |interface TreeNode {}
          |interface FormatInfo {}
        """.stripMargin))
    }

  def compile(output: ArtifactSource): ArtifactSource = {
    val withoutImport = TypeScriptInterfaceGeneratorTest.withoutImports(output)
    tsc.compile(withoutImport)
  }
}

class TypeScriptInterfaceGeneratorTest extends FlatSpec with Matchers {

  it should "generate compilable typescript file" in {
    val td = new TypeScriptInterfaceGenerator()
    // Make it put the generated files where our compiler will look for them
    val output = td.generate("", SimpleParameterValues(
      Map(td.OutputPathParam -> ".atomist/editors/Interfaces.ts")))
    assert(output.allFiles.size > 1)

    val compiled = TypeScriptInterfaceGeneratorTest.compile(output)
    val ts = compiled.allFiles.find(_.name.endsWith(".ts"))
    ts shouldBe defined
    // println(ts.get.content)

    val js = compiled.allFiles.find(_.name.endsWith(".js"))
    js shouldBe defined
    // println(js.get.content)
  }
}
