package com.atomist.rug.kind.elm

import com.atomist.rug.ts.RugTranspiler
import com.atomist.rug.{CompilerChainPipeline, RugPipeline}
import com.atomist.source.{SimpleFileBasedArtifactSource, StringFileArtifact}
import org.scalatest.{FlatSpec, Matchers}

class ElmTypescriptEditorTest extends FlatSpec with Matchers {

  val typeScriptPipeline: RugPipeline =
    new CompilerChainPipeline(Seq(new RugTranspiler()))

  it should "run the editor yo" in {
    val result = ElmTypeUsageTest.elmExecute(
      new SimpleFileBasedArtifactSource(
        "useless name",
        StringFileArtifact(
          "elm-package.json",
          ElmTypescriptEditorTestResources.elmPackageDotJson)),
      ElmTypescriptEditorTestResources.editor,
      runtime = typeScriptPipeline)
  }
}

object ElmTypescriptEditorTestResources {
  val editor =
    """
      |import {ParameterlessProjectEditor} from "user-model/operations/ProjectEditor"
      |import {Parameters} from "user-model/operations/Parameters"
      |import {Status, Result} from "user-model/operations/Result"
      |import {Project} from 'user-model/model/Core'
      |import {editor} from 'user-model/support/Metadata'
      |
      |@editor("Release editor")
      |class Release extends ParameterlessProjectEditor  {
      |
      |  editWithoutParameters(project: Project): Result {
      |     if (!project.fileExists("README.md")) {
      |       project.addFile("README.md", `# ${project.name()}
      |
      |Description Goes Here
      |       `);
      |     }
      |
      |     var readme = project.files();
      |
      |     return new Result(Status.Success, "yay woo");
      |
      |  }
      |
      |}
      | """.stripMargin

  val description = "i am the description yo"
  val org = "my-org"
  val repo = "project-repo"
  val elmPackageDotJson =
    s"""{
        |    "version": "1.0.0",
        |    "summary": "$description",
        |    "repository": "https://github.com/$org/$repo.git",
        |    "license": "BSD3",
        |    "source-directories": [
        |        "src"
        |    ],
        |    "exposed-modules": [],
        |    "dependencies": {
        |        "elm-lang/dom": "1.1.1 <= v < 2.0.0",
        |        "elm-lang/mouse": "1.0.0 <= v < 2.0.0",
        |        "elm-lang/core": "5.0.0 <= v < 6.0.0",
        |        "elm-lang/html": "2.0.0 <= v < 3.0.0"
        |    },
        |    "elm-version": "0.18.0 <= v < 0.19.0"
        |}
        |"""
}
