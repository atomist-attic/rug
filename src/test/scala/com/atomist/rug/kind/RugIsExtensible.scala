package com.atomist.rug.kind


import com.atomist.rug.kind.grammar.TypeUnderFile
import org.scalatest.FunSpec

import _root_.scala.util.parsing.combinator.RegexParsers


/**
  * We can create Language Extensions for Rug
  * and then use them to parse and navigate files.
  *
  * This test shows an extension with custom Scala parsing code.
  */

class PandaRugLanguageExtension extends TypeUnderFile {

  import com.atomist.source.FileArtifact
  import com.atomist.tree.content.text.PositionedTreeNode

  override val name: String = "Panda" // To trigger parsing, get to a file in the path expression, then /Panda()

  override def isOfType(f: FileArtifact): Boolean = f.name.endsWith(".panda")

  override def fileToRawNode(f: FileArtifact): Option[PositionedTreeNode] =
    Some(PandaParser.parse(f.content))

  override def description: String = "A language extension for my imaginary language"

}

object Panda {
  val PandaText =
    """panda { panda panda } panda
      |(panda panda) panda
    """.stripMargin

  val KawaiiPandas =
    """panda { kawaii kawaii } panda
      |(panda panda) panda
    """.stripMargin

  val PandaFilename = "my.panda"

  import com.atomist.source.{SimpleFileBasedArtifactSource, StringFileArtifact}

  val PandaProject = SimpleFileBasedArtifactSource(StringFileArtifact(PandaFilename, PandaText))

}

object PandaParser extends RegexParsers {

  import com.atomist.tree.content.text.{OffsetInputPosition, PositionedTreeNode}

  trait MinimalPositionedTreeNode extends PositionedTreeNode {

    import com.atomist.tree.TreeNode

    def name: String

    def valueOption: Option[String]

    def start: Int

    def end: Int

    override def nodeName = name

    override def startPosition = OffsetInputPosition(start)

    override def endPosition = OffsetInputPosition(end)

    override def value = valueOption.getOrElse("")

    override def childNodeNames: Set[String] = ???

    override def childNodeTypes: Set[String] = ???

    override def childrenNamed(key: String): Seq[TreeNode] = childNodes.filter(_.nodeName == key)

  }

  case class PositionedSyntaxNode(name: String,
                                  override val childNodes: Seq[PositionedSyntaxNode],
                                  override val valueOption: Option[String],
                                  start: Int,
                                  end: Int) extends MinimalPositionedTreeNode


  case class SyntaxNode(name: String,
                        childNodes: Seq[PositionedSyntaxNode],
                        valueOption: Option[String])

  object SyntaxNode {

    def leaf(name: String)(value: String): SyntaxNode =
      SyntaxNode(name, Seq(), Some(value))

    def parent(name: String, children: Seq[PositionedSyntaxNode]): SyntaxNode =
      SyntaxNode(name, children, None)
  }

  def positionedNode(inner: Parser[SyntaxNode]) = new Parser[PositionedSyntaxNode] {
    override def apply(in: Input): ParseResult[PositionedSyntaxNode] = {
      val start = handleWhiteSpace(in.source, in.offset)
      inner.apply(in) match {
        case Error(msg, next) => Error(msg, next)
        case Failure(msg, next) => Failure(msg, next)
        case Success(result, next) =>
          val end = next.offset
          Success(PositionedSyntaxNode(result.name, result.childNodes, result.valueOption,
            start, end), next)
      }
    }
  }

  def curly: Parser[PositionedSyntaxNode] =
    positionedNode("{" ~> rep(word) <~ "}" ^^ { n => SyntaxNode.parent("curly", n) })

  def paren: Parser[PositionedSyntaxNode] =
    positionedNode("(" ~> rep(word) <~ ")" ^^ { n => SyntaxNode.parent("paren", n) })

  def word: Parser[PositionedSyntaxNode] =
    positionedNode("[a-zA-Z]+".r ^^ SyntaxNode.leaf("word"))

  def pandaParser: Parser[PositionedSyntaxNode] =
    positionedNode(rep(word | curly | paren) ^^ { parts => SyntaxNode.parent("panda", parts) })

  def parse(content: String): PositionedSyntaxNode = {
    parseAll(pandaParser, content) match {
      case Success(result, next) => result
      case z: NoSuccess => throw new IllegalArgumentException("Could not parse as panda: " + z)
    }
  }
}

class RugIsExtensible extends FunSpec {

  describe("How rug can be extended to parse a panda language") {

    it("Can run an editor that uses the panda extension") {

      // TODO: get this Panda type declared in the manifest thinger somehow

      val changedProject = runTypeScriptProgram(Panda.PandaProject,
        "com/atomist/rug/kind/PandaParsingTypeScriptTest.ts",
        Map("changePandasInCurliesTo" -> "kawaii"))

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
