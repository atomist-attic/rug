package com.atomist.rug.kind.elm

import com.atomist.rug.ts.RugTranspiler
import com.atomist.rug.{CompilerChainPipeline, RugPipeline}
import com.atomist.source.{SimpleFileBasedArtifactSource, StringFileArtifact}
import org.scalatest.{FlatSpec, Matchers}

class ElmTypeScriptEditorTest extends FlatSpec with Matchers {

  import ElmTypeScriptEditorTestResources._

  val typeScriptPipeline: RugPipeline =
    new CompilerChainPipeline(Seq(new RugTranspiler()))

  it should "produce a README with a link" in {
    val projectName = "Elminess"
    val after = ElmTypeUsageTest.elmExecute(
      singleFileArtifactSource(projectName),
      Editor,
      runtime = typeScriptPipeline)

    val maybeReadme = after.findFile("README.md")
    maybeReadme.isDefined should be(true)
    val readme : String = maybeReadme.get.content

    withClue(s"README content----------\n$readme\n----------\n") {
      readme.contains( s"# ${projectName}") should be(true)

      readme.contains(s"\n${description}\n") should be(true)

     // readme.contains(s"https://${org}.github.io/${repo}") should be(true)
    }

  }

  def singleFileArtifactSource(projectName: String): SimpleFileBasedArtifactSource = {
    new SimpleFileBasedArtifactSource(
      projectName,
      StringFileArtifact(
        "elm-package.json",
        elmPackageDotJson))
  }
}

object ElmTypeScriptEditorTestResources {

  val Editor =
    """
      |import {ParameterlessProjectEditor} from "user-model/operations/ProjectEditor"
      |import {Parameters} from "user-model/operations/Parameters"
      |import {Status, Result} from "user-model/operations/Result"
      |import {Project} from 'user-model/model/Core'
      |import {Match,PathExpression,PathExpressionEngine,TreeNode} from 'user-model/tree/PathExpression'
      |
      |import {editor, inject} from '@atomist/rug/support/Metadata'
      |
      |@editor("Release editor")
      |class Release extends ParameterlessProjectEditor  {
      |
      |    private eng: PathExpressionEngine;
      |
      |    constructor(@inject("PathExpressionEngine") _eng: PathExpressionEngine ){
      |      super();
      |      this.eng = _eng;
      |    }
      |
      |    editWithoutParameters(project: Project): Result {
      |
      |    let pe = new PathExpression<Project,TreeNode>(
      |     `/*:file[name='elm-package.json']->json/summary/[1]`)
      |    let description: TreeNode = this.eng.scalar(project, pe)
      |
      |     if (!project.fileExists("README.md")) {
      |       project.addFile("README.md", `# ${project.name()}
      |
      |${description.value()}
      |       `);
      |     }
      |
      |     var readme = project.files();
      |
      |     return new Result(Status.Success, "yay woo");
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
        |""".stripMargin
}
