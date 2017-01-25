package com.atomist.rug

import com.atomist.param.SimpleParameterValues
import com.atomist.rug.InterpreterRugPipeline.DefaultRugArchive
import com.atomist.rug.TestUtils.doModification
import com.atomist.rug.compiler.typescript.TypeScriptCompilationException
import com.atomist.rug.kind.DefaultTypeRegistry
import com.atomist.rug.ts.{RugTranspiler, TypeScriptBuilder}
import com.atomist.source.{ArtifactSource, EmptyArtifactSource, SimpleFileBasedArtifactSource, StringFileArtifact}

/**
  * Uses the new transpiler
  */
class CompilerChainRuntimeTest extends AbstractRuntimeTest {

  override val pipeline: RugPipeline = new CompilerChainPipeline(Seq(TypeScriptBuilder.compiler, new RugTranspiler()))

  it should "handle custom kind of 'Line' nested under file" in {
    val program =
      """
        |@description "Documentation is good. Did I mention that I like documentation?"
        |editor LineCommenter
        |
        |with File fx
        | when isJava
        |with Line l1
        | when {
        |   return l1.num() == 0 && l1.content().indexOf("class") >= 0
        |  }
        |do
        |  eval { l1.update("// " + l1.content()) }
      """.stripMargin

    val programAs = new SimpleFileBasedArtifactSource(DefaultRugArchive, StringFileArtifact(pipeline.defaultFilenameFor(program), program))

    val poa = SimpleParameterValues(Map(
      "text" -> extraText,
      "message" -> "say this"))
    val r = doModification(programAs + TypeScriptBuilder.userModel, RugCompilerTest.JavaAndText, EmptyArtifactSource(), poa, pipeline)
    val f = r.findFile("src/main/java/Dog.java").get
    f.content.lines.size should be > (0)
    f.content.lines.forall(_.startsWith("// ")) should be(true)
  }

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
         |with File file
         | #when { file.name().endsWith(".java") }
         |  when { file.name().match(".java$$") }
         |do
         | append "$extraText"
      """.stripMargin
    simpleAppenderProgramExpectingParameters(goBowling, pipeline = pipeline)
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
    an[TypeScriptCompilationException] should be thrownBy
      simpleAppenderProgramExpectingParameters(goBowling, pipeline = pipeline)
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
        |with NotThing t
        |  do merge "simple.vm" "src/main/java/Dog.java";
      """.stripMargin
    val expected = "class Dog {}"
    try {
      simpleAppenderProgramExpectingParameters(goBowling, Some(expected), JavaAndTemplate, rugAs, pipeline = pipeline)
      fail("Should have failed due to unknown kind")
    }
    catch {
      // TODO was BadRugException
      case micturation: TypeScriptCompilationException =>
        micturation.getMessage.contains("NotThing") should be(true)
    }
  }
}

/**
  * Tests that still use the old interpreter: We are gradually migrating them.
  * So this class shows some of the remaining gaps in Rug transpiler support.
  */
class InterpreterRuntimeTest extends AbstractRuntimeTest {

  import RugCompilerTest._

  override val pipeline: InterpreterRugPipeline = new DefaultRugPipeline(DefaultTypeRegistry)

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

}
