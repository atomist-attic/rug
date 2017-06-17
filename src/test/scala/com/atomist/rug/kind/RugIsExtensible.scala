package com.atomist.rug.kind

import com.atomist.rug.kind.grammar.TypeUnderFile
import org.scalatest.FunSpec

import _root_.scala.util.parsing.combinator.RegexParsers

/**
  * We can create Language Extensions for Rug
  * and then use them to parse and navigate files.
  *
  * This test shows an extension with custom Scala parsing code.
  *
  * First, the language extension itself!
  * The key method here is fileToRawNode, which returns a ParsedNode, which is a tree
  * of nodes with position information.
  *
  * preprocess and postprocess are optional. the Panda language is whitespace
  * sensitive, so they're useful.
  *
  * There is one piece you can't see: this class is declared in
  * src/test/resources/META-INF/services/com.atomist.rug.spi.Typed
  */
class PandaRugLanguageExtension extends TypeUnderFile {

  import com.atomist.rug.kind.grammar.ParsedNode
  import com.atomist.source.FileArtifact

  override val name: String = "Panda" // To trigger parsing, get to a file in the path expression, then /Panda()

  override def isOfType(f: FileArtifact): Boolean = f.name.endsWith(".panda")

  override def fileToRawNode(f: FileArtifact): Option[ParsedNode] =
    Some(PandaParser.parse(f.content))

  override def description: String = "A language extension for my imaginary language"

  // mark significant beginnings of lines, ones that don't start with whitespace
  override def preprocess(originalContent: String): String =
    originalContent.replaceAll("(?m)^(\\S)", "\\$$1")

  // strip the preprocess marks
  override def postprocess(preprocessedContent: String): String =
    preprocessedContent.replaceAll("(?m)^\\$", "")

}

/**
  * Here is the parser! It uses Scala parser-combinators.
  */
object PandaParser extends RegexParsers {

  import com.atomist.rug.kind.grammar.{ParsedNode, SimpleParsedNode}

  type PositionedSyntaxNode = ParsedNode

  case class SyntaxNode(name: String,
                        childNodes: Seq[ParsedNode] = Seq())

  def positionedNode(inner: Parser[SyntaxNode]) = new Parser[PositionedSyntaxNode] {
    override def apply(in: Input): ParseResult[PositionedSyntaxNode] = {
      val start = handleWhiteSpace(in.source, in.offset)
      inner.apply(in) match {
        case Error(msg, next) => Error(msg, next)
        case Failure(msg, next) => Failure(msg, next)
        case Success(result, next) =>
          val end = next.offset
          Success(SimpleParsedNode(result.name, start, end, result.childNodes), next)
      }
    }
  }

  def line: Parser[PositionedSyntaxNode] =
    positionedNode("$" ~> rep(word | curly | paren) ^^ { parts => SyntaxNode("line", parts) })

  def curly: Parser[PositionedSyntaxNode] =
    positionedNode("{" ~> rep(word) <~ "}" ^^ { n => SyntaxNode("curly", n) })

  def paren: Parser[PositionedSyntaxNode] =
    positionedNode("(" ~> rep(word) <~ ")" ^^ { n => SyntaxNode("paren", n) })

  def word: Parser[PositionedSyntaxNode] =
    positionedNode("[a-zA-Z]+".r ^^ { _ => SyntaxNode("word") })

  def pandaParser: Parser[PositionedSyntaxNode] =
    positionedNode(rep(line) ^^ { parts => SyntaxNode("panda", parts) })

  def parse(content: String): PositionedSyntaxNode = {
    parseAll(pandaParser, content) match {
      case Success(result, next) => result
      case z: NoSuccess => throw new IllegalArgumentException("Could not parse as panda: " + z)
    }
  }
}

/**
  * These are just constants for test.
  */
object Panda {

  val PandaText =
    """panda { panda panda } panda
      | (panda panda) panda
      |
      |panda panda
    """.stripMargin

  val KawaiiPandas =
    """bear { kawaii kawaii } panda
      | (panda panda) panda
      |
      |bear panda
    """.stripMargin

  val PandaFilename = "my.panda"

  import com.atomist.source.{SimpleFileBasedArtifactSource, StringFileArtifact}

  val PandaProject = SimpleFileBasedArtifactSource(StringFileArtifact(PandaFilename, PandaText))

}

/**
  * Here is the end-to-end test! See the TypeScript program, it lives in src/test/resources.
  * This test checks that it converts the input to the output.
  */
class RugIsExtensible extends FunSpec {

  describe("How rug can be extended to parse a panda language") {

    it("Can run an editor that uses the panda extension") {
      // TODO: get this Panda type declared in the manifest thinger somehow
      val changedProject = runTypeScriptProgram(
        Panda.PandaProject,
        "com/atomist/rug/kind/PandaParsingTypeScriptTest.ts",
        Map(
          "changePandasInCurliesTo" -> "kawaii",
          "changeFirstPandaInEachLineTo" -> "bear"))

      assert(changedProject.findFile(Panda.PandaFilename).get.content == Panda.KawaiiPandas)
    }
  }

  import com.atomist.source.ArtifactSource

  def runTypeScriptProgram(target: ArtifactSource, tsEditorResource: String, parameterMap: Map[String, String]): ArtifactSource = {
    import com.atomist.param.SimpleParameterValues
    import com.atomist.project.edit.SuccessfulModification
    import com.atomist.rug.RugArchiveReader
    import com.atomist.rug.ts.TypeScriptBuilder
    import com.atomist.source.file.ClassPathArtifactSource

    val parameters = SimpleParameterValues.fromMap(parameterMap)

    // construct the Rug archive
    val artifactSourceWithEditor = ClassPathArtifactSource.toArtifactSource(tsEditorResource).withPathAbove(".atomist/editors")
    val artifactSourceWithRugNpmModule = TypeScriptBuilder.compileWithModel(artifactSourceWithEditor)

    // get the operation out of the artifact source
    val projectEditor = RugArchiveReader(artifactSourceWithRugNpmModule).editors.head

    // apply the operation
    projectEditor.modify(target, parameters) match {
      case sm: SuccessfulModification =>
        sm.result
      case boo => fail(s"Modification was not successful: $boo")
    }
  }
}

/**
  * Here is another way to write high-level tests for your language extension.
  * This will load files in a directory, and you can run path expressions
  * against them.
  */
class PandaRugLanguageExtensionText extends FunSpec with RugLanguageExtensionTest {

  it("can change my source code") {
    val sourceProjectLocation = "src/test/resources"

    val pmv = projectFromDirectory(sourceProjectLocation)

    val expr = """/com/atomist/rug/kind/Sample.panda/Panda()/line/curly/word"""

    val nodes = evaluatePathExpression(pmv, expr)

    nodes.head.update("xiaoping")

    val newContent = pmv.findFile("com/atomist/rug/kind/Sample.panda").content
    assert(newContent.contains("xiaoping"))

    // println("New content: ------\n" + newContent + "\n--------")
  }
}
