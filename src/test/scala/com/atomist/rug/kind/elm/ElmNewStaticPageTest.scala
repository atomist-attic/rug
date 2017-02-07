package com.atomist.rug.kind.elm

import java.io.File

import com.atomist.project.SimpleProjectOperationArguments
import com.atomist.project.archive.DefaultAtomistConfig
import com.atomist.project.edit.ProjectEditor
import com.atomist.project.generate.EditorInvokingProjectGenerator
import com.atomist.rug.kind.DefaultTypeRegistry
import com.atomist.rug.parser.ParserCombinatorRugParser
import com.atomist.rug.runtime.rugdsl.DefaultEvaluator
import com.atomist.rug.{DefaultRugCompiler, InterpreterRugPipeline}
import com.atomist.source.ArtifactSource
import com.atomist.source.file.{FileSystemArtifactSource, FileSystemArtifactSourceIdentifier}
import org.scalatest.{FlatSpec, Matchers}
import org.scalatest.OptionValues._

class ElmNewStaticPageTest extends FlatSpec with Matchers {

  it should "create an artifact source" in {
    val projectName = "Projectitron"
    val description = "Clever project of smartness"
    val org = "my-org"
    val freshArtifactSource = invokeGenerator("elm-start-static-page", "NewStaticPage",
      Map("project_name" -> projectName,
          "description" -> description,
          "org" -> org))

    val elmPackageDotJsonOption = freshArtifactSource.findFile("elm-package.json")

    val elmPackageDotJson = elmPackageDotJsonOption.value

    elmPackageDotJson.content should include(s""""summary": "${description}"""")

    val repositoryLine = s""""repository": "https://github.com/${org}/${projectName.toLowerCase()}.git","""
    elmPackageDotJson.content should include(repositoryLine)
  }

  private def invokeGenerator(rugArchiveDirectoryOnTheClasspath: String,
                      generatorName: String,
                      parameters: Map[String, Object]) = {
    val pipeline =
      new InterpreterRugPipeline(
        new ParserCombinatorRugParser(),
        new DefaultRugCompiler(DefaultEvaluator, DefaultTypeRegistry),
        DefaultAtomistConfig)

    val rugArchive = archiveFromDirectoryOnClasspath(rugArchiveDirectoryOnTheClasspath)
    val ops = pipeline.create(rugArchive, None)
    val generatingEditor = ops.find { _.name == generatorName }.collect { case x : ProjectEditor => x}

    val projectEditor = withClue(ops.map(_.name)) {
      generatingEditor.value
    }

    val gen =
      new EditorInvokingProjectGenerator(
        "StaticPage",
        projectEditor,
        rugArchive)

    val freshArtifactSource =
      gen.generate("",
        SimpleProjectOperationArguments(
          "wut",
          parameters))
    freshArtifactSource
  }

  private def archiveFromDirectoryOnClasspath(resourceName: String): ArtifactSource = {
    val whereIsIt = this.getClass.getClassLoader.getResource(resourceName)
    if (whereIsIt == null) {
      throw new RuntimeException(s"Unable to find ${resourceName} on the classpath")
    }
    new FileSystemArtifactSource(FileSystemArtifactSourceIdentifier(new File(whereIsIt.toURI)))
  }
}
