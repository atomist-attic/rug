package com.atomist.rug.compiler

import com.atomist.source.{ArtifactSourceUtils, ArtifactSource, SimpleFileBasedArtifactSource, StringFileArtifact}
import org.scalatest.{FlatSpec, Matchers}

class ServiceLoaderCompilerRegistryTest extends FlatSpec with Matchers {

  def compilerFor(as: ArtifactSource): Compiler = {
    val tsc = ServiceLoaderCompilerRegistry.findAll(as)
    tsc.headOption.getOrElse(fail("Should have found compiler"))

    //new TypeScriptCompiler
  }


  it should "compile a simple editor" in {
    val as = SimpleFileBasedArtifactSource(StringFileArtifact(".atomist/Thing.ts",
      """
        |import {ProjectEditor} from 'user-model/operations/ProjectEditor'
        |import {Parameters} from 'user-model/operations/Parameters'
        |import {Project} from 'user-model/model/Core'
        |
        |class TestEditor implements ProjectEditor<Parameters> {
        |    edit(p: Project) {
        |       return "";
        |    }
        |}
      """.stripMargin))
    val tsc = compilerFor(as)
    val compiled = tsc.compile(as)
    for (f <- compiled.allFiles) {
      println(f.path)
      println(f.content)
      println
    }

  }

}
