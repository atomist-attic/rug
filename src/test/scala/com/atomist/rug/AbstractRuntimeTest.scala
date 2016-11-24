package com.atomist.rug

import com.atomist.project.SimpleProjectOperationArguments
import com.atomist.project.edit.{SuccessfulModification, ProjectEditor}
import com.atomist.rug.kind.core.FileArtifactMutableView
import com.atomist.rug.runtime.LambdaPredicate
import com.atomist.source.file.SimpleFileSystemArtifactSourceIdentifier
import com.atomist.source.{SimpleFileBasedArtifactSource, StringFileArtifact, ArtifactSource, EmptyArtifactSource}
import org.scalatest.{Matchers, FlatSpec}

import com.atomist.rug.RugCompilerTest._
import com.atomist.rug.TestUtils._


abstract class AbstractRuntimeTest extends FlatSpec with Matchers {

  protected def pipeline: RugPipeline

  val extraText = "// I'm talkin' about ethics"

  it should "prepend text" in {
    val license = "This is the world's shortest open source license. Do what you want."
    val program =
      s"""
         |editor AddHeader
         |
         |with file
         | do prepend "$license"
         |
      """.stripMargin

    val r = doModification(program, JavaAndText, EmptyArtifactSource(""), SimpleProjectOperationArguments.Empty, pipeline)
    r.allFiles.size should be > (0)
    r.allFiles.foreach(f => f.content.contains(license) should be(true))
  }

  it should "execute simple program with = literal in predicate" in {
    val goBowling =
      """
        |@description "A very short tempered editor"
        |editor Caspar
        |
        |param text: .*
        |param message: .*
        |
        |with file f
        | when name = "Dog.java"
        |do
        | append text
      """.stripMargin
    simpleAppenderProgramExpectingParameters(goBowling, pipeline = pipeline)
  }

  it should "execute simple program with parameters and transform function using param identifier" in {
    val goBowling =
      """
        |@description "A very short tempered editor"
        |editor Caspar
        |
        |param text: .*
        |param message: .*
        |
        |with file f
        | when true
        |do
        | append text
      """.stripMargin
    simpleAppenderProgramExpectingParameters(goBowling, pipeline = pipeline)
  }

  it should "accept extra parameter containing -" in {
    val goBowling =
      """
        |@description "A very short tempered editor"
        |editor Caspar
        |
        |param text: .*
        |param message: .*
        |
        |with file f
        | when true
        |do
        | append { text + "" }
      """.stripMargin
    simpleAppenderProgramExpectingParameters(goBowling, extraParams = Map(
      "team-id" -> "T117KLCSK"
    ), pipeline = pipeline)
  }

  it should "accept extra parameters containing - and JavaScript" in {
    val goBowling =
      """
        |@description "A very short tempered editor"
        |editor Caspar
        |
        |param text: .*
        |param message: .*
        |
        |with file f
        | when true
        |do
        | append { text + "" }
      """.stripMargin
    simpleAppenderProgramExpectingParameters(goBowling, extraParams = Map(
      "team-id" -> "T117KLCSK",
      "member_id" -> "M0001"
    ), pipeline = pipeline)
  }

  it should "execute simple program with parameters and JavaScript transform" in {
    val goBowling =
      """
        |@description "I can get you a toe!"
        |editor Caspar
        |
        |param text: .*
        |param message: .*
        |
        |with file f
        |do
        |  setContent { f.content() + text }
      """.stripMargin
    simpleAppenderProgramExpectingParameters(goBowling, pipeline = pipeline)
  }

  it should "execute simple program with parameters and JavaScript transform in 2 steps" in {
    val goBowling =
      """
        |@description "I can get you a toe!"
        |editor Caspar
        |
        |param text: .*
        |param message: .*
        |
        |with file f
        | when true
        |begin
        |  do setContent { "WWW" + f.content() + text }
        |  do setContent { f.content().substring(3) }
        |end
      """.stripMargin
    simpleAppenderProgramExpectingParameters(goBowling, pipeline = pipeline)
  }



  it should "execute simple program with parameters and transform function using computed identifier" in {
    val goBowling =
      """
        |@description 'A very short tempered editor'
        |editor Caspar
        |
        |param message: .*
        |
        |let text = "// I'm talkin' about ethics"
        |
        |with file f
        | when true
        |do
        | append text
      """.stripMargin
    simpleAppenderProgramExpectingParameters(goBowling, pipeline = pipeline)
  }

  it should "execute simple program with parameters and transform function using JavaScript computed identifier" in {
    val goBowling =
      """
        |@description 'A very short tempered editor'
        |editor Caspar
        |
        |param message: .*
        |
        |let text = { "// I'm talkin' about ethics" }
        |let random = { "" + message }
        |
        |with file f
        | when true
        |do
        | append text
      """.stripMargin
    simpleAppenderProgramExpectingParameters(goBowling, pipeline = pipeline)
  }

  it should "execute simple program with true comparison" in {
    val goBowling =
      """
        |@description "I can get you a toe!"
        |editor Caspar
        |
        |with file f when isJava = true
        |do
        |  replace "Dog" "Cat";
      """.stripMargin
    val originalFile = JavaAndText.findFile("src/main/java/Dog.java").get
    val expected = originalFile.content.replace("Dog", "Cat")
    simpleAppenderProgramExpectingParameters(goBowling, Some(expected), pipeline = pipeline)
  }

  it should "execute simple program with false comparison preventing bad changes" in {
    val goBowling =
      """
        |@description "I can get you a toe!"
        |editor Caspar
        |
        |with file f when isJava = true
        |do
        |  replace "Dog" "Cat"
        |
        |# better not fire
        |with file f when isJava = false
        |do
        |  replace "Cat" "Squirrel"
      """.stripMargin
    val originalFile = JavaAndText.findFile("src/main/java/Dog.java").get
    val expected = originalFile.content.replace("Dog", "Cat")
    simpleAppenderProgramExpectingParameters(goBowling, Some(expected), pipeline = pipeline)
  }

  it should "execute simple program with directory transformation" in {
    val DefaultPackageJavaAndText: ArtifactSource = SimpleFileBasedArtifactSource(
        StringFileArtifact("pom.xml", "<maven></maven"),
        StringFileArtifact("Dog.java", """class Dog {}""".stripMargin)
    )
    val goBowling =
      """
        |@description "I can get you a toe!"
        |editor Caspar
        |
        |with project p;
        |do
        |  moveUnder "src/main/java";
      """.stripMargin
    val originalFile = DefaultPackageJavaAndText.findFile("Dog.java").get
    val expected = originalFile.content
    simpleAppenderProgramExpectingParameters(goBowling, Some(expected),
      DefaultPackageJavaAndText, pipeline = pipeline)
  }

  // TODO this depends on resolution from the type registry rather than from
  // simple descent
  it should "handle custom kind of 'line' nested under file" in pendingUntilFixed {
    val program =
      """
        |@description "Documentation is good. Did I mention that I like documentation?"
        |editor LineCommenter
        |
        |with file fx
        | when isJava
        |with line l1
        | when {
        |   return l1.num() == 0 && l1.content().indexOf("class") > 0
        |  }
        |do
        |  eval { l1.update("// " + l1.content()) }
      """.stripMargin

    val fr = FixedRugFunctionRegistry(
      Map(
        "isJava" -> new LambdaPredicate[FileArtifactMutableView]("isJava", f => f.currentBackingObject.name.endsWith(".java"))
      )
    )
    val originalFile = JavaAndText.findFile("src/main/java/Dog.java").get

    val eds = pipeline.createFromString(program)
    eds.size should be(1)
    val pe = eds.head.asInstanceOf[ProjectEditor]

    val r = pe.modify(RugCompilerTest.JavaAndText, SimpleProjectOperationArguments("", Map(
      "text" -> extraText,
      "message" -> "say this")))
    r match {
      case sm: SuccessfulModification =>
        val f = sm.result.findFile("src/main/java/Dog.java").get
        f.content.lines.size should be > (0)
        f.content.lines.forall(_.startsWith("// ")) should be(true)
    }
  }

  protected def simpleAppenderProgramExpectingParameters(program: String,
                                                         finalFileContent: Option[String] = None,
                                                         as: ArtifactSource = JavaAndText,
                                                         rugAs: ArtifactSource = EmptyArtifactSource(""),
                                                         extraParams: Map[String, String] = Map(),
                                                         pipeline: RugPipeline = new DefaultRugPipeline()) = {
    // val originalFile = as.findFile("src/main/java/Dog.java").get
    val poa = SimpleProjectOperationArguments("", Map(
      "text" -> extraText,
      "message" -> "say this") ++ extraParams)
    val r = doModification(program, as, rugAs, poa, pipeline)
    val path = "src/main/java/Dog.java"
    val fO = r.findFile(path)
    if (fO.isEmpty)
      fail(s"Cannot find file at $path")
    val f = fO.get
    f.content should equal(finalFileContent.getOrElse(
      as.findFile("src/main/java/Dog.java").get.content + extraText))
  }
}
