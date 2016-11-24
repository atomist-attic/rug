package com.atomist.rug

import com.atomist.project.SimpleProjectOperationArguments
import com.atomist.project.edit.{ProjectEditor, SuccessfulModification}
import com.atomist.rug.kind.DefaultTypeRegistry
import com.atomist.rug.kind.core.FileArtifactMutableView
import com.atomist.rug.runtime.LambdaPredicate
import com.atomist.rug.ts.RugTranspiler
import com.atomist.source.{ArtifactSource, SimpleFileBasedArtifactSource, StringFileArtifact}

/**
  * Uses the new transpiler
  */
class CompilerChainRuntimeTest extends AbstractRuntimeTest {

  override val pipeline: RugPipeline = new CompilerChainPipeline(Seq(new RugTranspiler()))


  // Note that we do not support .endsWith
  it should "execute simple program with parameters, simple JavaScript file function and transform function using default type " in {
    val goBowling =
      s"""
         |@description "That's over the line!"
         |editor Caspar
         |
         |param text: .*
         |param message: .*
         |
         |with file
         | #when { file.name().endsWith(".java") }
         |  when { file.name().match(".java$$") }
         |do
         | append "$extraText"
      """.stripMargin
    simpleAppenderProgramExpectingParameters(goBowling, pipeline = pipeline)
  }
}


/**
  * Uses the old interpreter
  */
class InterpreterRuntimeTest extends AbstractRuntimeTest {

  import RugCompilerTest._
  import TestUtils._

  override val pipeline: InterpreterRugPipeline = new DefaultRugPipeline(DefaultTypeRegistry)

  it should "execute simple program with parameters using dot notation in predicate" in {
    val goBowling =
      """
        |@description 'A very short tempered editor'
        |editor Caspar
        |
        |param message: .*
        |
        |let text = "// I'm talkin' about ethics"
        |
        |with file f when f.name = "Dog.java"
        |do
        | append text
      """.stripMargin
    simpleAppenderProgramExpectingParameters(goBowling)
  }

  it should "execute simple program with parameters using dot notation with longer path in predicate" in {
    val goBowling =
      """
        |@description 'A very short tempered editor'
        |editor Caspar
        |
        |param message: .*
        |
        |let text = "// I'm talkin' about ethics"
        |
        |with file f when f.name.contains "Dog"
        |do
        | append text
      """.stripMargin
    simpleAppenderProgramExpectingParameters(goBowling)
  }

  it should "execute simple program with parameters using JavaScript action block" in {
    val goBowling =
      """
        |editor Caspar
        |
        |param text: .*
        |param message: .*
        |
        |{
        |
        |for (i = 0; i < project.files().length; i++) {
        |    var currentFile = project.files().get(i);
        |    if (currentFile.isJava()) {
        |        currentFile.append(text);
        |    }
        |}
        |
        |}
      """.stripMargin
    simpleAppenderProgramExpectingParameters(goBowling)
  }

  it should "execute simple program with parameters and transform function using from extractor" in pendingUntilFixed {
    val goBowling =
      """
        |@description 'A very short tempered editor'
        |editor Caspar
        |
        |param message: .*
        |
        |let text = from file f when path = "message.txt" return content
        |
        |with file f
        | when isJava
        |do
        | append text
      """.stripMargin
    simpleAppenderProgramExpectingParameters(goBowling)
  }

  it should "fail to execute simple program with parameters and transform function using undefined identifier" in {
    val goBowling =
      """
        |@description "I believe in the first amendment!"
        |editor Caspar
        |
        |param text: .*
        |param message: .*
        |
        |with file f
        | when isJava
        |do
        | append undefined_identifier
      """.stripMargin
    an[BadRugException] should be thrownBy simpleAppenderProgramExpectingParameters(goBowling)
  }

  it should "execute simple program with parameters, simple JavaScript file function and transform function" in {
    val goBowling =
      s"""
         |@description "That's over the line!"
         |editor Caspar
         |
         |param text: .*
         |param message: .*
         |
         |with file f
         | when { f.name().endsWith(".java") }
         |do
         | append "$extraText"
      """.stripMargin
    simpleAppenderProgramExpectingParameters(goBowling)
  }


  it should "execute simple program with parameters, simple JavaScript file function using computed value and transform function" in {
    val goBowling =
      s"""
         |@description "That's over the line!"
         |editor Caspar
         |
         |let extension = ".java"
         |
         |with file f
         | when { f.name().endsWith(extension) }
         |do
         | append "$extraText"
      """.stripMargin
    simpleAppenderProgramExpectingParameters(goBowling)
  }

  it should "execute simple program with parameters, complex JavaScript file function and transform function" in {
    val goBowling =
      s"""
         |@description "What is this, the high hat?"
         |editor Caspar
         |
         |param text: .*
         |param message: .*
         |
         |with file f
         | when {
         |  var flag = true;
         |  if (1 > 2) {
         |      // println("Something");
         |  }
         |  return f.name().endsWith(".java") && flag;
         | }
         |do
         | append "$extraText"
      """.stripMargin
    simpleAppenderProgramExpectingParameters(goBowling)
  }

  it should "execute simple program with illegal JavaScript parameter name" in {
    val goBowling =
      """
        |@description "I can get you a toe!"
        |editor Caspar
        |
        |param text: .*
        |param message: .*
        |
        |with file f
        | when isJava
        |begin
        |  do setContent { "WWW" + f.content() + params['text'] }
        |  do setContent { f.content().substring(3) }
        |end
      """.stripMargin
    simpleAppenderProgramExpectingParameters(goBowling, extraParams = Map("this-is-bad" -> "whatever"))
  }

  it should "execute simple program with parameters and multiple with blocks applying to same file" in {
    val goBowling =
      """
        |@description "I can get you a toe!"
        |editor Caspar
        |
        |param text: .*
        |param message: .*
        |
        |with file f
        | when isJava
        |do
        |  setContent { "WWW" + f.content() + params['text'] } ;
        |
        |with file f
        | when isJava
        |do
        |  setContent { f.content().substring(3) };
      """.stripMargin
    simpleAppenderProgramExpectingParameters(goBowling)
  }

  it should "execute simple program with parameters and JavaScript regexp transform" in {
    val goBowling =
      """
        |@description "I can get you a toe!"
        |editor Caspar
        |
        |param text: .*
        |param message: .*
        |
        |with file f
        | when isJava
        |do
        |  setContent {
        |   var re = /Dog/gi;
        |   return f.content().replace(re, 'Cat');
        |   };
      """.stripMargin
    val originalFile = JavaAndText.findFile("src/main/java/Dog.java").get
    val expected = originalFile.content.replace("Dog", "Cat")
    simpleAppenderProgramExpectingParameters(goBowling, Some(expected))
  }

  it should "execute simple program with regexp transform via globals" in {
    val goBowling =
      """
        |@description "I can get you a toe!"
        |editor Caspar
        |
        |with project p
        |do
        |  replace "Dog" "Cat"
      """.stripMargin
    val originalFile = JavaAndText.findFile("src/main/java/Dog.java").get
    val expected = originalFile.content.replace("Dog", "Cat")
    simpleAppenderProgramExpectingParameters(goBowling, Some(expected))
  }

  it should "allow int let value in call other operation" in {
    val goBowling =
      """
        |@description "I can get you a toe!"
        |editor Caspar
        |
        |let x = 2
        |
        |Other num = x
        |
        |
        |@description "This is a second editor"
        |editor Other
        |
        |param num: \d+
        |
        |with project p
        |do
        |  replace "Dog" num
      """.stripMargin
    val originalFile = JavaAndText.findFile("src/main/java/Dog.java").get
    val expected = originalFile.content.replace("Dog", "2")
    simpleAppenderProgramExpectingParameters(goBowling, Some(expected))
  }

  it should "allow let with same name as parameter in call other operation" in {
    val goBowling =
      """
        |@description "I can get you a toe!"
        |editor Caspar
        |
        |let num = 2
        |
        |Other num = num
        |
        |@description "This is a second editor"
        |editor Other
        |
        |param num: \d+
        |
        |with project p
        |do
        |  replace "Dog" num
      """.stripMargin
    val originalFile = JavaAndText.findFile("src/main/java/Dog.java").get
    val expected = originalFile.content.replace("Dog", "2")
    simpleAppenderProgramExpectingParameters(goBowling, Some(expected))
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
        |with project p;
        |do
        |  merge "simple.vm" "src/main/java/Dog.java";
      """.stripMargin
    // TODO idea allow words like "to"   merge "simple.vm" to "src/main/java";
    val expected = "class Dog {}"
    simpleAppenderProgramExpectingParameters(goBowling, Some(expected), JavaAndTemplate, rugAs)
  }

  it should "raise appropriate error when no such kind module" in {
    val JavaAndTemplate: ArtifactSource = new SimpleFileBasedArtifactSource("name",
      StringFileArtifact("pom.xml", "<maven></maven")
    )
    val rugAs = new SimpleFileBasedArtifactSource("rugs",
      StringFileArtifact("templates/simple.vm", """class Dog {}""")
    )
    val goBowling =
      """
        |@description "I can get you a toe!"
        |editor Caspar
        |
        |with thing t;
        |do
        |  merge "simple.vm" "src/main/java/Dog.java";
      """.stripMargin
    // TODO idea allow words like "to"   merge "simple.vm" to "src/main/java";
    val expected = "class Dog {}"
    try {
      simpleAppenderProgramExpectingParameters(goBowling, Some(expected), JavaAndTemplate, rugAs)
      fail("Should have failed due to unknown kind")
    }
    catch {
      case micturation: BadRugException =>
        micturation.getMessage.contains("thing") should be(true)
    }
  }

  // PUT in a deliberate type error and expect to see a good message
  it should "handle type error in predicate and give good error message" is pending

}
