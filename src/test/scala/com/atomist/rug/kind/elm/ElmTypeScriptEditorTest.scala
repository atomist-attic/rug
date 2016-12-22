package com.atomist.rug.kind.elm

import com.atomist.rug.compiler.typescript.TypeScriptCompiler
import com.atomist.rug.compiler.typescript.compilation.CompilerFactory
import com.atomist.rug.ts.RugTranspiler
import com.atomist.rug.{CompilerChainPipeline, RugPipeline}
import com.atomist.source.{SimpleFileBasedArtifactSource, StringFileArtifact}
import org.scalatest.{FlatSpec, Matchers}

class ElmTypeScriptEditorTest extends FlatSpec with Matchers {

  import ElmTypeScriptEditorTestResources._

  val typeScriptPipeline: RugPipeline =
    new CompilerChainPipeline(Seq(new RugTranspiler(), new TypeScriptCompiler(CompilerFactory.create())))

  it should "produce a README with a link" in {
    val projectName = "Elminess"
    val after = ElmTypeUsageTest.elmExecute(
      singleFileArtifactSource(projectName),
      ReleaseEditor,
      runtime = typeScriptPipeline)

    val maybeReadme = after.findFile("README.md")
    maybeReadme.isDefined should be(true)
    val readme : String = maybeReadme.get.content

    withClue(s"README content----------\n$readme\n----------\n") {
      readme.contains( s"# ${projectName}") should be(true)

      readme.contains(s"${System.lineSeparator()}${description}${System.lineSeparator()}") should be(true)

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

  val ReleaseEditor: String =
    """
      |import {Status, Result} from "@atomist/rug/operations/RugOperation"
      |import {Project} from '@atomist/rug/model/Core'
      |import {ProjectEditor} from '@atomist/rug/operations/ProjectEditor'
      |import {Match,PathExpression,PathExpressionEngine,TreeNode} from '@atomist/rug/tree/PathExpression'
      |
      |class Release implements ProjectEditor  {
      |
      |    name: string = "Release"
      |    description: string  ="Release editor"
      |
      |    edit(project: Project): Result {
      |
      |    let eng: PathExpressionEngine = project.context().pathExpressionEngine();
      |
      |    let pe = new PathExpression<Project,TreeNode>(
      |     `/File()[@name='elm-package.json']/Json()/summary/*[1]`)
      |    let description: TreeNode = eng.scalar(project, pe)
      |
      |     if (!project.fileExists("README.md")) {
      |       project.addFile("README.md", `# ${project.name()}
      |
      |${description.value()}
      |       `);
      |     }
      |
      |     var readme = project.files();
      |     return new Result(Status.Success, "yay woo");
      |  }
      |
      |}
      |var editor = new Release()
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
