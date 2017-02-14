package com.atomist.rug.kind.rug

import com.atomist.rug.kind.DefaultTypeRegistry
import com.atomist.rug.kind.core.ProjectMutableView
import com.atomist.rug.kind.dynamic.MutableContainerMutableView
import com.atomist.rug.kind.rug.dsl.RugFileType
import com.atomist.source.{ArtifactSource, EmptyArtifactSource, SimpleFileBasedArtifactSource, StringFileArtifact}
import com.atomist.tree.content.text.OverwritableTextTreeNode
import com.atomist.tree.pathexpression.{ExpressionEngine, PathExpressionEngine, PathExpressionParser}
import com.atomist.tree.utils.TreeNodeUtils
import org.scalatest.{FlatSpec, Matchers}

object RugFileTypeTest {

  val HelloProject =
    """editor HelloProject
      |
      |with Project p
      |  do eval { "Hello: " + p.name(); }
    """.stripMargin

  val DumbGenerator =
    """generator DumbGenerator
      |
      |with Project p
      |  do eval { "Hello: " + p.name(); }
    """.stripMargin

  val DumberGenerator =
    """generator DumberGenerator
      |
      |uses DumbGenerator
      |
      |with Project p
      |  do eval { "Hello: " + p.name(); }
    """.stripMargin

  val SomeEditors =
    """editor FirstEditor
      |
      |# TODO: what is the format ... and can I add to these in a kind like RugType
      |param rug_name: ^.*$
      |
      |with RugArchiveProject p begin
      |  #do eval { print("The rug name is " + rug_name) }
      |  with Editor r when r.name = rug_name begin
      |    #do eval { print("Changing rug " + r.name() ) }
      |    do r.convertToTypeScript
      |  end
      |end
      |
      |editor SecondEditor
      |
      |with Project p
      |  do eval { p.name(); }
      |""".stripMargin

  val ManyParams =
    """editor MyEditor
      |
      |@tag "python"
      |@tag "python3"
      |@tag "flask"
      |@description "creates a new Python Flask Microservice project"
      |@generator "NewFlaskMicroserviceProject"
      |editor NewFlaskMicroserviceProject
      |
      |@displayName "Project name"
      |@description "names your project following Python conventions"
      |@validInput "A valid Python name respecting Python conventions (PEP 423)."
      |@minLength 1
      |@maxLength 21
      |param project_name: ^[A-Za-z][-A-Za-z0-9_]+$
      |
      |@displayName "Application name"
      |@description "names your Flask application following PEP8 conventions for package naming"
      |@validInput "A valid Python package name respecting PEP8."
      |param app_name: ^[A-Za-z][A-Za-z0-9_]+$
      |
      |@displayName "Semantic Version"
      |@description "sets a Semantic Version number of the form, e.g. 0.1.0"
      |@validInput "a valid semantic version, http://semver.org"
      |@optional
      |@default "0.1.0"
      |param version: @semantic_version
      |
      |
      |with Project p
      |  do eval { p.name(); }
    """.stripMargin

  val UsesVarious =
    """editor UsesVarious
      |
      |uses atomist-rugs.flask-service.AddDockerfile
      |uses atomist-rugs.flask-service.AddTravis
      |
      |with Project p
      |  do eval { p.name(); }
    """.stripMargin

  val SomeUsingSemver =
    """editor WithSemVerEditor
      |
      |param version: @semantic_version
      |
      |with Project p
      |  do eval { p.name(); }
      |
      |editor WithoutSemVerEditor
      |
      |with Project p
      |  do eval { p.name(); }
      |
      |editor WithSemVerEditorAgain
      |
      |param version: @semantic_version
      |
      |with Project p
      |  do eval { p.name(); }
    """.stripMargin

  val UsingOldGeneratorAnnotation =
    """@generator "UberGenerator"
      |editor UberGenerator
      |
      |with Project p
      |  do eval { p.name(); }
    """.stripMargin

  val HelloProjectEditor: ArtifactSource = new SimpleFileBasedArtifactSource("name",
    Seq(
      StringFileArtifact("HelloProject.rug", HelloProject)
    ))


  val DumbAndDumberGenerator: ArtifactSource = new SimpleFileBasedArtifactSource("name",
    Seq(
      StringFileArtifact("DumbGenerator.rug", DumbGenerator),
      StringFileArtifact("DumberGenerator.rug", DumberGenerator)
    ))


  val TwoEditors: ArtifactSource = new SimpleFileBasedArtifactSource("name",
    Seq(
      StringFileArtifact("TwoEditors.rug", SomeEditors)
    ))


  val ManyParamsEditor: ArtifactSource = new SimpleFileBasedArtifactSource("name",
    Seq(
      StringFileArtifact("ManyParamsEditor.rug", ManyParams)
    ))


  val UsesVariousEditor: ArtifactSource = new SimpleFileBasedArtifactSource("name",
    Seq(
      StringFileArtifact("UsesVariousEditor.rug", UsesVarious)
    ))


  val SomeUsingSemverEditor: ArtifactSource = new SimpleFileBasedArtifactSource("name",
    Seq(
      StringFileArtifact("SomeUsingSemverEditor.rug", SomeUsingSemver)
    ))


  val UsingOldGeneratorAnnotationEditor: ArtifactSource = new SimpleFileBasedArtifactSource("name",
    Seq(
      StringFileArtifact("UsingOldGeneratorAnnotationEditor.rug", UsingOldGeneratorAnnotation)
    ))




  def helloProjectEditorProject = new ProjectMutableView(EmptyArtifactSource(),
    HelloProjectEditor)


  def multiRugsInASingleRugFile = new ProjectMutableView(EmptyArtifactSource(),
    TwoEditors)


  def rugArchive = new ProjectMutableView(EmptyArtifactSource(),
    HelloProjectEditor + DumbAndDumberGenerator + TwoEditors + UsingOldGeneratorAnnotationEditor)

}

class RugFileTypeTest extends FlatSpec with Matchers {

  import RugFileTypeTest._

  val ee: ExpressionEngine = new PathExpressionEngine


  val rugFileType = new RugFileType


  it should "load a basic Rug" in {
    val rugs = rugFileType.findAllIn(helloProjectEditorProject)
    assert(rugs.size === 1)
  }

  it should "parse a Rug into mutable view and write out unchanged" in {
    val rugs = rugFileType.findAllIn(helloProjectEditorProject)
    assert(rugs.size === 1)
    rugs.head.head match {
      case mtn: MutableContainerMutableView =>
        val content = mtn.value
        content should equal(helloProjectEditorProject.files.get(0).content)
      case u: OverwritableTextTreeNode => assert(u.value === helloProjectEditorProject.files.get(0).content)
    }
  }

  it should "find HelloProject using path expression" in {
    val expr = "//RugFile()"
    val rtn = ee.evaluate(helloProjectEditorProject, PathExpressionParser.parseString(expr), DefaultTypeRegistry)
    assert(rtn.right.get.size === 1)
  }

  it should "find HelloProject using a predicate path expression" in {
    val expr = "//File[RugFile()]"
    val rtn = ee.evaluate(helloProjectEditorProject, PathExpressionParser.parseString(expr), DefaultTypeRegistry)
    assert(rtn.right.get.size === 1)
  }

  it should "find all editors in a single Rug" in {
    val expr = "/RugFile()//rug"
    val rtn = ee.evaluate(multiRugsInASingleRugFile, PathExpressionParser.parseString(expr), DefaultTypeRegistry)
    assert(rtn.right.get.size === 2)
  }

  it should "find only generators in a Rug archive" in {
    val expr = "//RugFile()[/rug/type[@value='generator']]"
    val rtn = ee.evaluate(rugArchive, PathExpressionParser.parseString(expr), DefaultTypeRegistry)
    assert(rtn.right.get.size === 2)
  }

  it should "find Rugs using DumbGenerator" in {
    val expr = "//RugFile()[/rug/uses/other_rug[@value='DumbGenerator']]"
    val rtn = ee.evaluate(rugArchive, PathExpressionParser.parseString(expr), DefaultTypeRegistry)
    withClue(s"I found ${rtn.right.get.map(TreeNodeUtils.toShortString)}") {
      assert(rtn.right.get.size === 1)
    }
  }
}
