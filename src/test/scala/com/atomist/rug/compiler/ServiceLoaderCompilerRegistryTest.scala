package com.atomist.rug.compiler

import com.atomist.rug.ts.TypeScriptBuilder
import com.atomist.source.{SimpleFileBasedArtifactSource, StringFileArtifact}
import com.typesafe.scalalogging.LazyLogging
import org.scalatest.{FlatSpec, Matchers}

class ServiceLoaderCompilerRegistryTest extends FlatSpec with Matchers with LazyLogging {

  it should "compile a simple editor" in {
    val as = SimpleFileBasedArtifactSource(StringFileArtifact(".atomist/Thing.ts",
      """
        |import {Editor} from '@atomist/rug/operations/Decorators'
        |import {Project} from '@atomist/rug/model/Core'
        |
        |@Editor("Nothing special")
        |class TestEditor  {
        |    edit(p: Project) {
        |       // Do nothing
        |    }
        |}
      """.stripMargin))

    val compiled = TypeScriptBuilder.compileWithModel(as)
    for (f <- compiled.allFiles) {
      logger.debug(f.path)
    }
  }
}
