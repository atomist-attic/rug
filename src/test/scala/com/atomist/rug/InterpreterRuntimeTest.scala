package com.atomist.rug

import com.atomist.rug.kind.DefaultTypeRegistry
import com.atomist.rug.ts.RugTranspiler
import com.atomist.source.{ArtifactSource, SimpleFileBasedArtifactSource, StringFileArtifact}

/**
  * Uses the new transpiler
  */
class CompilerChainRuntimeTest extends AbstractRuntimeTest {

  override val pipeline: RugPipeline = new CompilerChainPipeline(Seq(new RugTranspiler()))

  // Note that we do not support .endsWith
  it should "execute simple program with parameters, simple JavaScript file function and transform function using default type" in {
    val goBowling =
      s"""
         |@description "That's over the line!"
         |editor Caspar
         |
         |param text: ^.*$$
         |param message: ^.*$$
         |
         |with File
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

  override val pipeline: InterpreterRugPipeline = new DefaultRugPipeline(DefaultTypeRegistry)

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
    simpleAppenderProgramExpectingParameters(goBowling)
  }

  it should "execute simple program with parameters using dot notation with longer path in predicate" in {
    val goBowling =
      """
        |@description 'A very short tempered editor'
        |editor Caspar
        |
        |param message: ^.*$
        |
        |let text = "// I'm talkin' about ethics"
        |
        |with File f when f.name.contains "Dog"
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
        |param text: ^.*$
        |param message: ^.*$
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

  it should "fail to execute simple program with parameters and transform function using undefined identifier" in {
    val goBowling =
      """
        |@description "I believe in the first amendment!"
        |editor Caspar
        |
        |param text: ^.*$
        |param message: ^.*$
        |
        |with File f
        | when isJava
        |do
        | append undefined_identifier
      """.stripMargin
    an[BadRugException] should be thrownBy simpleAppenderProgramExpectingParameters(goBowling)
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
         |param text: ^.*$$
         |param message: ^.*$$
         |
         |with File f
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
        |param text: ^.*$
        |param message: ^.*$
        |
        |with File f
        | when isJava
        |begin
        |  do setContent { "WWW" + f.content() + params['text'] }
        |  do setContent { f.content().substring(3) }
        |end
      """.stripMargin
    simpleAppenderProgramExpectingParameters(goBowling, extraParams = Map("this-is-bad" -> "whatever"))
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
    simpleAppenderProgramExpectingParameters(goBowling, Some(expected))
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
        |param num: ^\d+$
        |
        |with Project p
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
        |param num: ^\d+$
        |
        |with Project p
        |do
        |  replace "Dog" num
      """.stripMargin
    val originalFile = JavaAndText.findFile("src/main/java/Dog.java").get
    val expected = originalFile.content.replace("Dog", "2")
    simpleAppenderProgramExpectingParameters(goBowling, Some(expected))
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

  it should "run editor with complicated regular expression" in {
    val complexRegexEditor =
      """editor ComplexRegexpReplace
        |
        |with project p
        |  do regexpReplace "^\\s*class\\s+Dog\\s*\\{\\s*\\}" "ssalc Dog {}"
      """.stripMargin
    val originalFile = JavaAndText.findFile("src/main/java/Dog.java").get
    val expected = originalFile.content.replace("class", "ssalc")
    simpleAppenderProgramExpectingParameters(complexRegexEditor, Some(expected))
  }

  // PUT in a deliberate type error and expect to see a good message
  it should "handle type error in predicate and give good error message" is pending

}
