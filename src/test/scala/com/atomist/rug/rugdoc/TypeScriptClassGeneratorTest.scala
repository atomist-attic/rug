package com.atomist.rug.rugdoc

import com.atomist.param.SimpleParameterValues
import com.atomist.rug.spi.SimpleTypeRegistry
import com.atomist.rug.ts._
import com.atomist.source.{ArtifactSource, FileArtifact, FileEditor}
import org.scalatest.{FlatSpec, Matchers}

object TypeScriptClassGeneratorTest {

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
      f.withContent(f.content.replace(InterfaceGenerationConfig.TestStubImports,
        """
          |interface ProjectContext {}
          |interface PathExpressionEngine {}
          |interface TreeNode {}
          |interface FormatInfo {}
          |interface Addressed {}
          |abstract class AddressedNodeSupport implements Addressed {
          |
          |    private _address: string = null
          |
          |    address() { return this._address }
          |
          |    setAddress(addr: string) {
          |        this._address = addr
          |    }
          |
          |}
        """.stripMargin))
    }

  def compile(output: ArtifactSource): ArtifactSource = {
    val withoutImport = TypeScriptClassGeneratorTest.withoutImports(output)
//    for {
//      f <- withoutImport.allFiles
//      if f.name.endsWith(".ts")
//    } {
//      println(f.path)
//      println(f.content)
//    }
    tsc.compile(withoutImport)
  }
}

class TypeScriptClassGeneratorTest extends FlatSpec with Matchers {

  // Note, only external model is relevant.
  // We don't need to test against project types like File
  it should "generate compilable typescript classes" in {

    val types = new TypeGenerator().extract(TypeGeneratorTest.CortexJson)
    val tr = new SimpleTypeRegistry(types)

    val td = new TypeScriptClassGenerator(tr)
    // Make it put the generated files where our compiler will look for them
    val output = td.generate("", SimpleParameterValues(
      Map(td.outputPathParam -> ".atomist/editors/Interfaces.ts")))
    assert(output.allFiles.size > 1)

    val compiled = TypeScriptClassGeneratorTest.compile(output)
    val ts = compiled.allFiles.find(_.name.endsWith(".ts"))
    ts shouldBe defined
    // println(ts.get.content)

    val js = compiled.allFiles.find(_.name.endsWith(".js"))
    js shouldBe defined

    val js2 = compiled.allFiles.find(_.content.contains("withResolvedBy"))
    js2 shouldBe defined
    // println(js.get.content)
  }
}
