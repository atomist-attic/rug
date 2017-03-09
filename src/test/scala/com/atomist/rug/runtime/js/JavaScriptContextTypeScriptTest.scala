package com.atomist.rug.runtime.js

import com.atomist.param.SimpleParameterValues
import com.atomist.project.archive.RugArchiveReader
import com.atomist.parse.java.ParsingTargets
import com.atomist.project.edit.SuccessfulModification
import com.atomist.rug.ts.TypeScriptBuilder
import com.atomist.source.file.ClassPathArtifactSource
import org.scalatest.{FlatSpec, Matchers}

class JavaScriptContextTypeScriptTest extends FlatSpec with Matchers {

  it should "use JavaScriptContext from TypeScript" in {

    val tsEditorResource = "com/atomist/rug/runtime/js/JavaScriptContextTypeScriptTest.ts"
    val parameters = SimpleParameterValues.Empty
    val target = ParsingTargets.NewStartSpringIoProject
    val fileThatWillBeModified = "hello.txt"

    // construct the Rug archive
    val artifactSourceWithEditor = ClassPathArtifactSource.toArtifactSource(tsEditorResource).withPathAbove(".atomist/generators")
    val artifactSourceWithRugNpmModule = TypeScriptBuilder.compileWithModel(artifactSourceWithEditor)
    //println(s"rug archive: $artifactSourceWithEditor")

    // get the operation out of the artifact source
    val projectGenerator = RugArchiveReader.find(artifactSourceWithRugNpmModule).generators.head

    // apply the operation
    val result = projectGenerator.generate("poe", parameters)
    val contents = result.findFile(fileThatWillBeModified).get.content
    withClue(s"contents of $fileThatWillBeModified are:<$contents>") {
      // check the results
      contents.contains("hello yo") should be(true)
    }

  }

}
