package com.atomist.rug.compiler

import com.atomist.rug.TestUtils
import com.atomist.source.{SimpleFileBasedArtifactSource, StringFileArtifact}
import com.typesafe.scalalogging.LazyLogging
import org.scalatest.{FlatSpec, Matchers}

class ServiceLoaderCompilerRegistryTest extends FlatSpec with Matchers with LazyLogging {

  it should "compile a simple editor" in {
    val as = SimpleFileBasedArtifactSource(StringFileArtifact(".atomist/Thing.ts",
      """
        |import {ProjectEditor} from '@atomist/rug/operations/ProjectEditor'
        |import {Project} from '@atomist/rug/model/Core'
        |
        |class TestEditor implements ProjectEditor {
        |    name: string = "TestEditor"
        |    description: string = "Nothing special"
        |    edit(p: Project) {
        |       // Do nothing
        |    }
        |}
      """.stripMargin))

    val compiled = TestUtils.compileWithModel(as)
    for (f <- compiled.allFiles) {
      logger.debug(f.path)
    }
  }
}
