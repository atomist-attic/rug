package com.atomist.rug.rugdoc

import com.atomist.param.SimpleParameterValues
import com.atomist.rug.spi.SimpleTypeRegistry
import com.atomist.rug.ts._
import com.atomist.source.{ArtifactSource, FileArtifact, FileEditor}
import org.scalatest.{FlatSpec, Matchers}
import CortexTypeGenerator._

object TypeScriptStubClassGeneratorTest {

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
      f.withContent(f.content
        .replace(TypeGenerationConfig.DefaultImports,
          TypeScriptInterfaceGeneratorTest.InterfaceTestImports)
        .replace(TypeGenerationConfig.TestStubImports,
          s"""
             |interface GraphNode {}
        """.stripMargin))
    }

  def compile(output: ArtifactSource): ArtifactSource = {
    val withoutImport = TypeScriptStubClassGeneratorTest.withoutImports(output)
    tsc.compile(withoutImport)
  }
}

class TypeScriptStubClassGeneratorTest extends FlatSpec with Matchers {

  import DefaultTypeGeneratorConfig.CortexJson

  // Note, only external model is relevant.
  // We don't need to test against project types like File
  it should "generate compilable typescript classes" in {
    val types = new CortexTypeGenerator(DefaultCortexDir, DefaultCortexStubDir).extract(CortexJson)
    val tr = new SimpleTypeRegistry(types)

    // Classes won't compile without the interfaces they implement
    val tid = new TypeScriptInterfaceGenerator(tr)
    // Make it put the generated files where our compiler will look for them
    val interfaces = tid.generate("", SimpleParameterValues(
      Map(AbstractTypeScriptGenerator.OutputPathParam -> ".atomist/editors/Interfaces.ts")))

    val td = new TypeScriptStubClassGenerator(tr)
    // Make it put the generated files where our compiler will look for them
    val output = td.generate("", SimpleParameterValues(
      Map(AbstractTypeScriptGenerator.OutputPathParam -> ".atomist/editors/stubs/Interfaces.ts")))
    assert(output.allFiles.size > 1)

    val compiled = TypeScriptStubClassGeneratorTest.compile(interfaces + output)
    val ts = compiled.allFiles.find(_.name.endsWith(".ts"))
    ts shouldBe defined
    // println(ts.get.content)

    val js = compiled.allFiles.find(_.name.endsWith(".js"))
    js shouldBe defined

    //val js2 = compiled.allFiles.find(_.content.contains("withResolvedBy"))
    //js2 shouldBe defined
    // println(js.get.content)
  }
}
