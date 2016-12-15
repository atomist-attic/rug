package com.atomist.rug.compiler

import com.atomist.source.{ArtifactSource, SimpleFileBasedArtifactSource, StringFileArtifact}
import com.typesafe.scalalogging.LazyLogging
import org.scalatest.{FlatSpec, Matchers}

class ServiceLoaderCompilerRegistryTest extends FlatSpec with Matchers with LazyLogging {

  def compilerFor(as: ArtifactSource): Compiler = {
    val tsc = ServiceLoaderCompilerRegistry.findAll(as)
    tsc.headOption.getOrElse(fail("Should have found compiler"))
  }

  it should "compile a simple editor" in {
    val as = SimpleFileBasedArtifactSource(StringFileArtifact(".atomist/Thing.ts",
      """
        |import {ProjectEditor} from 'user-model/operations/ProjectEditor'
        |import {Project} from 'user-model/model/Core'
        |import {Result,Status} from 'user-model/operations/RugOperation'
        |
        |class TestEditor implements ProjectEditor {
        |    name: string = "TestEditor"
        |    description: string = "Nothing special"
        |    edit(p: Project) {
        |       return new Result(Status.Success, "Boom!");
        |    }
        |}
      """.stripMargin))
    val tsc = compilerFor(as)
    val compiled = tsc.compile(as)
    for (f <- compiled.allFiles) {
      logger.debug(f.path)
    }
  }
}
