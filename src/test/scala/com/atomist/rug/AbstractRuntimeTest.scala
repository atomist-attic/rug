package com.atomist.rug

import com.atomist.param.SimpleParameterValues
import com.atomist.rug.InterpreterRugPipeline.DefaultRugArchive
import com.atomist.rug.RugCompilerTest._
import com.atomist.rug.TestUtils._
import com.atomist.rug.ts.TypeScriptBuilder
import com.atomist.source.{ArtifactSource, EmptyArtifactSource, SimpleFileBasedArtifactSource, StringFileArtifact}
import org.scalatest.{Assertion, FlatSpec, Matchers}

abstract class AbstractRuntimeTest extends FlatSpec with Matchers {

  protected  def pipeline: RugPipeline

  val extraText = "// I'm talkin' about ethics"

  it should "prepend text" in {
    val license = "This is the world's shortest open source license. Do what you want."
    val program =
      s"""
         |editor AddHeader
         |
         |with File
         | do prepend "$license"
         |
      """.stripMargin

    def as = new SimpleFileBasedArtifactSource(DefaultRugArchive, StringFileArtifact(pipeline.defaultFilenameFor(program), program))  + TypeScriptBuilder.userModel

    val r = doModification(as, JavaAndText, EmptyArtifactSource(""), SimpleParameterValues.Empty, pipeline)
    r.allFiles.size should be > (0)
    r.allFiles.foreach(f => f.content.contains(license) should be(true))
  }

  it should "execute simple program with parameters using dot notation in predicate" in {
    val goBowling =
      """
        |@description 'A very short tempered editor'
        |editor Caspar
        |
        |param message: ^.*$
        |
        |let text = "// I'm talkin' about ethics"
        |
        |with File f when f.name = "Dog.java"
        |do
        | append text
      """.stripMargin
    simpleAppenderProgramExpectingParameters(goBowling, pipeline = pipeline)
  }

  it should "execute simple program with = literal in predicate" in {
    val goBowling =
      """
        |@description "A very short tempered editor"
        |editor Caspar
        |
        |param text: @any
        |param message: @any
        |
        |with File f
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
        |param text: @any
        |param message: @any
        |
        |with File f
        | when true
        |do
        | append text
      """.stripMargin
    simpleAppenderProgramExpectingParameters(goBowling, pipeline = pipeline)
  }

  it should "execute simple program with parameters and JavaScript regexp transform" in {
    val goBowling =
      """
        |@description "I can get you a toe!"
        |editor Caspar
        |
        |param text: ^.*$
        |param message: ^.*$
        |
        |with File f
        | when isJava
        |do
        |  setContent {
        |   var re = /Dog/gi;
        |   return f.content().replace(re, 'Cat');
        |   };
      """.stripMargin
    val originalFile = JavaAndText.findFile("src/main/java/Dog.java").get
    val expected = originalFile.content.replace("Dog", "Cat")
    simpleAppenderProgramExpectingParameters(goBowling, Some(expected), pipeline = pipeline)
  }

  it should "accept extra parameter containing -" in {
    val goBowling =
      """
        |@description "A very short tempered editor"
        |editor Caspar
        |
        |param text: ^.*$
        |param message: ^.*$
        |
        |with File f
        | when true
        |   do append { text + "" }
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
        |param text: ^.*$
        |param message: ^.*$
        |
        |with File f
        | when true
        |do
        | append { text + "" }
      """.stripMargin
    simpleAppenderProgramExpectingParameters(goBowling, extraParams = Map(
      "team-id" -> "T117KLCSK",
      "member_id" -> "M0001"
    ), pipeline = pipeline)
  }

  it should "execute simple program with illegal JavaScript parameter name" in {
    val goBowling =
      """
        |@description "I can get you a toe!"
        |editor Caspar
        |
        |param text: ^.*$
        |param message: ^.*$
        |
        |with File f
        | when isJava
        |begin
        |  do setContent { "WWW" + f.content() + text }
        |  do setContent { f.content().substring(3) }
        |end
      """.stripMargin
    simpleAppenderProgramExpectingParameters(goBowling,
      extraParams = Map("this-is-bad" -> "whatever"),
      pipeline = pipeline)
  }

  it should "execute simple program with parameters and JavaScript transform" in {
    val goBowling =
      """
        |@description "I can get you a toe!"
        |editor Caspar
        |
        |param text: ^.*$
        |param message: ^.*$
        |
        |with File f
        |do
        |  setContent { f.content() + text }
      """.stripMargin
    simpleAppenderProgramExpectingParameters(goBowling, pipeline = pipeline)
  }

  it should "execute simple program with parameters, simple JavaScript file function using computed value and transform function" in {
    val goBowling =
      s"""
         |@description "That's over the line!"
         |editor Caspar
         |
         |let extension = ".java"
         |
         |with File f
         | when { f.name().indexOf(extension) > 0 }
         |do
         | append "$extraText"
      """.stripMargin
    simpleAppenderProgramExpectingParameters(goBowling, pipeline = pipeline)
  }

  it should "execute simple program with parameters and JavaScript transform in 2 steps" in {
    val goBowling =
      """
        |@description "I can get you a toe!"
        |editor Caspar
        |
        |param text: ^.*$
        |param message: ^.*$
        |
        |with File f
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
        |param message: ^.*$
        |
        |let text = "// I'm talkin' about ethics"
        |
        |with File f
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
        |param message: ^.*$
        |
        |let text = { "// I'm talkin' about ethics" }
        |let random = { "" + message }
        |
        |with File f
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
        |with File f when isJava = true
        |do
        |  replace "Dog" "Cat";
      """.stripMargin
    val originalFile = JavaAndText.findFile("src/main/java/Dog.java").get
    val expected = originalFile.content.replace("Dog", "Cat")
    simpleAppenderProgramExpectingParameters(goBowling, Some(expected), pipeline = pipeline)
  }

  it should "execute simple program with parameters, complex JavaScript file function and transform function" in {
    val goBowling =
      s"""
         |@description "What is this, the high hat?"
         |editor Caspar
         |
         |param text: ^.*$$
         |param message: ^.*$$
         |
         |with File f
         | when {
         |  var flag = true;
         |  if (1 > 2) {
         |      // println("Something");
         |  }
         |  return f.name().indexOf(".java") != -1 && flag;
         | }
         |do
         | append "$extraText"
      """.stripMargin
    simpleAppenderProgramExpectingParameters(goBowling, pipeline = pipeline)
  }

  it should "execute simple program with regexp transform via globals" in {
    val goBowling =
      """
        |@description "I can get you a toe!"
        |editor Caspar
        |
        |with Project p
        |do
        |  replace "Dog" "Cat"
      """.stripMargin
    val originalFile = JavaAndText.findFile("src/main/java/Dog.java").get
    val expected = originalFile.content.replace("Dog", "Cat")
    simpleAppenderProgramExpectingParameters(goBowling, Some(expected), pipeline = pipeline)
  }

  it should "execute simple program with template interpretation" in {
    val JavaAndTemplate: ArtifactSource = new SimpleFileBasedArtifactSource("name",
      StringFileArtifact("pom.xml", "<maven></maven")
    )
    val rugAs = new SimpleFileBasedArtifactSource("rugs",
      StringFileArtifact(s"${atomistConfig.templatesRoot}/simple.vm", """class Dog {}""")
    )
    val goBowling =
      """
        |@description "I can get you a toe!"
        |editor Caspar
        |
        |with Project p;
        |do
        |  merge "simple.vm" "src/main/java/Dog.java";
      """.stripMargin
    val expected = "class Dog {}"
    simpleAppenderProgramExpectingParameters(goBowling, Some(expected), JavaAndTemplate, rugAs, pipeline = pipeline)
  }

  it should "preserve merge after copyFiles" in {
    val outputAs = EmptyArtifactSource("")
    val templatePath = "my_template.vm"
    val mergeOutputPath = "my_template.output"
    val rugAs = SimpleFileBasedArtifactSource(
      StringFileArtifact(".atomist/templates/" + templatePath, "content"),
      StringFileArtifact("test/foo", "file content")
    )
    val program =
      """
        |editor TestForMergeOverwrite
        |with Project p
        |  begin
        |    do merge 'my_template.vm' to 'my_template.output'
        |    do copyEditorBackingFilesWithNewRelativePath sourcePath='test/' destinationPath='test_out'
        |  end
      """.stripMargin
    val pas = new SimpleFileBasedArtifactSource(DefaultRugArchive, StringFileArtifact(pipeline.defaultFilenameFor(program), program))  + TypeScriptBuilder.userModel
    val r = doModification(pas, outputAs, rugAs, SimpleParameterValues.Empty, pipeline)
    assert(r.totalFileCount === 2)
    assert(r.findFile(mergeOutputPath).get.content === "content")
    assert(r.findFile("test_out/foo").get.content === "file content")
    assert(r.cachedDeltas.size === 3)
    r.cachedDeltas.exists(_.path == mergeOutputPath) should be(true)
    r.cachedDeltas.exists(_.path == "test_out") should be(true)
    r.cachedDeltas.exists(_.path == "test_out/foo") should be(true)
  }

  it should "execute simple program with false comparison preventing bad changes" in {
    val goBowling =
      """
        |@description "I can get you a toe!"
        |editor Caspar
        |
        |with File f when isJava = true
        |do
        |  replace "Dog" "Cat"
        |
        |# better not fire
        |with File f when isJava = false
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
        |with Project p;
        |do
        |  moveUnder "src/main/java";
      """.stripMargin
    val originalFile = DefaultPackageJavaAndText.findFile("Dog.java").get
    val expected = originalFile.content
    simpleAppenderProgramExpectingParameters(goBowling, Some(expected),
      DefaultPackageJavaAndText, pipeline = pipeline)
  }

  it should "execute simple program with parameters, simple JavaScript file function and transform function" in {
    val goBowling =
      s"""
         |@description "That's over the line!"
         |editor Caspar
         |
         |param text: ^.*$$
         |param message: ^.*$$
         |
         |with File f
         | when { /.*\\.java$$/.test(f.name())}
         |do
         | append "$extraText"
      """.stripMargin
    simpleAppenderProgramExpectingParameters(goBowling, pipeline = pipeline)
  }

  /*
  it should "allow int let value in call other operation" in {
    val goBowling =
      """
        |@description "I can get you a toe!"
        |editor Caspar
        |
        |Other num = 2
        |
        |@description "This is a second editor"
        |editor Other
        |
        |param num: ^\d+$
        |
        |with Project p
        |do
        |  replace "Dog" num
      """.stripMargin
    val originalFile = JavaAndText.findFile("src/main/java/Dog.java").get
    val expected = originalFile.content.replace("Dog", "2")
    simpleAppenderProgramExpectingParameters(goBowling, Some(expected), pipeline = pipeline)
  }

  it should "allow int let value in call other operation using parameter" in {
    val goBowling =
      """
        |@description "I can get you a toe!"
        |editor Caspar
        |
        |Other
        |
        |
        |@description "This is a second editor"
        |editor Other
        |
        |param num: ^\d+$
        |
        |with Project p
        |do
        |  replace "Dog" "2"
      """.stripMargin
    val originalFile = JavaAndText.findFile("src/main/java/Dog.java").get
    val expected = originalFile.content.replace("Dog", "2")
    simpleAppenderProgramExpectingParameters(goBowling, Some(expected), pipeline = pipeline)
  }
  */

  it should "run editor with complicated regular expression" in {
    val complexRegexEditor =
      """editor ComplexRegexpReplace
        |
        |with Project p
        |  do regexpReplace "^\\s*class\\s+Dog\\s*\\{\\s*\\}" "ssalc Dog {}"
      """.stripMargin
    val originalFile = JavaAndText.findFile("src/main/java/Dog.java").get
    val expected = originalFile.content.replace("class", "ssalc")
    simpleAppenderProgramExpectingParameters(complexRegexEditor, Some(expected))
  }

  it should "execute simple program with parameters and multiple with blocks applying to same file" in {
    val goBowling =
      """
        |@description "I can get you a toe!"
        |editor Caspar
        |
        |param text: ^.*$
        |param message: ^.*$
        |
        |with File f
        | when isJava
        |do
        |  setContent { "WWW" + f.content() + text } ;
        |
        |with File f
        | when isJava
        |do
        |  setContent { f.content().substring(3) };
      """.stripMargin
    simpleAppenderProgramExpectingParameters(goBowling, pipeline = pipeline)
  }

  protected  def simpleAppenderProgramExpectingParameters(program: String,
                                                         finalFileContent: Option[String] = None,
                                                         as: ArtifactSource = JavaAndText,
                                                         rugAs: ArtifactSource = EmptyArtifactSource(""),
                                                         extraParams: Map[String, String] = Map(),
                                                         pipeline: RugPipeline = new DefaultRugPipeline()): Assertion = {
    // val originalFile = as.findFile("src/main/java/Dog.java").get
    val poa = SimpleParameterValues(Map(
      "text" -> extraText,
      "message" -> "say this") ++ extraParams)
    val pas = new SimpleFileBasedArtifactSource(DefaultRugArchive, StringFileArtifact(pipeline.defaultFilenameFor(program), program))  + TypeScriptBuilder.userModel
    val r = doModification(pas, as, rugAs, poa, pipeline)
    val path = "src/main/java/Dog.java"
    val fO = r.findFile(path)
    if (fO.isEmpty)
      fail(s"Cannot find file at $path")
    val f = fO.get
    assert(f.content === finalFileContent.getOrElse(
      as.findFile("src/main/java/Dog.java").get.content + extraText))
  }
}
