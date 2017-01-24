package com.atomist.util.scalaparsing

import com.atomist.tree.content.text._
import com.atomist.rug.parser.RugParser._
import com.atomist.rug.{BadRugException, BadRugSyntaxException, RugRuntimeException}
import com.atomist.source.FileArtifact
import com.atomist.tree.pathexpression.PathExpression
import com.atomist.util.{Visitable, Visitor}
import com.typesafe.scalalogging.LazyLogging

import scala.util.matching.Regex
import scala.util.parsing.combinator.JavaTokenParsers
import scala.util.parsing.input.{CharSequenceReader, OffsetPosition, Positional}

/**
  * Convenient support for parsers for different languages implemented using
  * Scala parser combinators.
  * Offers common tokens, consistent reserved word handling, comment and string handling,
  * and productions for nested blocks.
  * However, the most important functionality allows convenient parsing into the
  * TreeNode structure for exposure via Rug.
  *
  * @see TreeNode
  */
abstract class CommonTypesParser extends JavaTokenParsers with LazyLogging {

  val CComment = """/\*([^*]|[\r\n]|(\*+([^*/]|[\r\n])))*\*+/"""

  val HashLineComment = """\#.*[\n|\r\n]"""

  /** White space honoring C style block comments and Python style # line comments */
  val CBlockCommentAndHashLineCommentWhitespace: Regex = ("""(\s|""" + HashLineComment + "|" + CComment + ")+").r

  // NB: This does not correctly preserve positions
  def doubleQuotedString: Parser[String] = (stringLiteral | ("\"" ~ "[^\"]+".r ~ "\"")) ^^ {
    case s : String => s.substring(1).dropRight(1).replace("""\\""", """\""")
    case _ ~ illFormedString ~ _ => throw new BadRugException(
      s"It looks like you're trying to use a string, but [$illFormedString] is not a valid Java String") {}
  }

  // Taken from Scala superclass
  def doubleQuotedStringContent: Parser[String] = """([^"\p{Cntrl}\\]|\\[\\'"bfnrt]|\\u[a-fA-F0-9]{4})*+""".r

  val PlusToken = "+"

  val EqualsToken = "="

  val LiteralDot = "."

  def singleQuote = "'"

  def singleQuotedString: Parser[String] = singleQuote ~> """[^']*""".r <~ singleQuote

  def tripleQuote: Parser[String] = "\"{3}".r

  val openBlock: Parser[String] = "{"

  val closeBlock: Parser[String] = "}"

  def javaPackage: Parser[String] = "([a-zA-Z_$][a-zA-Z\\d_$]*\\.)*[a-zA-Z_$][a-zA-Z\\d_$]*|^$".r

  /**
    * Must be invoked before a double quoted string
    */
  def tripleQuotedString: Parser[String] = new Parser[String] {
    private val SingleQuote = '"'

    override def apply(in: Input): ParseResult[String] = {
      val source = in.source
      val offset = in.offset
      val start = handleWhiteSpace(source, offset)

      if (start > source.length() - 2) {
        return Failure("Triple quoted string expected but not found", in.drop(start - offset))
      }

      if (!(source.charAt(start) == SingleQuote && source.charAt(start + 1) == SingleQuote && source.charAt(start + 2) == SingleQuote)) {
        return Failure("Triple quoted string expected but not found", in.drop(start - offset))
      }

      var j = start + 3
      var depth = 3

      trait State
      object SeenQuote extends State
      object NotSeenQuote extends State

      var state: State = SeenQuote

      do {
        val c = source.charAt(j)
        c match {
          case SingleQuote =>
            depth -= 1
            state match {
              case NotSeenQuote => state = SeenQuote
              case _ =>
            }
          case _ =>
            state match {
              case SeenQuote =>
                state = NotSeenQuote
                depth = 3
              case _ =>
            }
        }
        j += 1
      } while (depth > 0 && j < source.length())

      val matchedLength = j - start
      if (matchedLength > 6) {
        val tString = source.subSequence(start + 3, j - 3).toString
        Success(tString, in.drop(j - offset))
      } else {
        val found = if (start == source.length()) "end of source" else "`" + source.charAt(start) + "'"
        Failure("Triple quoted string expected but " + found + " found", in.drop(start - offset))
      }
    }
  }

  case class IdentifierLookup(id: String)

  // Lookup for an identifier beginning with @ such as @java_class in a well-known regular expression
  protected def identifierLookup: Parser[IdentifierLookup] = AtToken ~> ident ^^ (s => IdentifierLookup(s))

  // TODO should tighten this up
  def regexp: Parser[String] = """.*""".r

  def eof: Parser[String] = "\\Z".r

  def terminator: Parser[String] = ";"

  def javaScriptBlock: Parser[JavaScriptBlock] = escapedBlock("{", "}", depthCount = true, esc = "\\") ^^ (content => JavaScriptBlock(content))

  def grammarBlock: Parser[GrammarBlock] = escapedBlock("<", ">", depthCount = false, esc = "\\") ^^ (content => GrammarBlock(content))

  /**
    * Expect an escaped block, e.g. in {}
    * Content is anything that doesn't pop the stack of the escaping.
    *
    * @param esc        character that can be used to escape string
    * @param depthCount should we count the depth of tokens, for example in JavaScript
    *                   blocks where we allow { to nest
    * @return a parser
    */
  def escapedBlock(left: String, right: String, depthCount: Boolean, esc: String = ""): Parser[String] = new Parser[String] {
    // Used in debug output
    val description = s"Block enclosed with $left...$right"

    def apply(in: Input): ParseResult[String] = {
      val source = in.source
      val offset = in.offset
      val start = handleWhiteSpace(source, offset)
      var j = start

      var depth = 0
      // Content to save, which may excluded escaped characters
      var contentToSave = ""

      if (start == source.length()) {
        logger.debug("Parsed " + source.subSequence(start, j))
        return Failure(s"$description expected but not found", in.drop(start - offset))
      }

      // This character is escaped
      def escaped = esc.nonEmpty && j > (start + esc.length) &&
        source.toString.substring(0, j).endsWith(esc)

      val leftToken = left.charAt(0)
      val rightToken = right.charAt(0)
      do {
        val ch = source.charAt(j)
        ch match {
          case `leftToken` if (depth == 0 || depthCount) && !escaped => depth += 1
          case `rightToken` if !escaped => depth -= 1
          case `rightToken` if escaped =>
            // Knock out escape sequence from string
            contentToSave = contentToSave.dropRight(esc.length)
          case _ =>
        }
        j += 1
        contentToSave += ch
      } while (depth > 0 && j < source.length())

      val matchedLength = j - start

      if (matchedLength > left.length + right.length) {
        val block = // source.subSequence(start + 1, j - 1).toString
          contentToSave.substring(left.length).dropRight(right.length)
        logger.debug(s"Found $description: \n$block")
        Success(block, in.drop(j - offset))
      } else {
        val found = if (start == source.length()) "end of source" else "`" + source.charAt(start) + "'"
        Failure(s"$description expected but " + found + " found", in.drop(start - offset))
      }
    }
  }

  def capitalLetter: Parser[String] = "[A-Z]".r

  def lowercaseLetter: Parser[String] = "[a-z]".r

  /**
    * Must start with a capital letter.
    *
    * @return identifier value
    */
  def capitalizedIdentifier: Parser[String] = capitalLetter ~ opt(ident) ^^ {
    case l ~ Some(rest) => l + rest
    case l ~ None => l
  }

  def camelCaseIdentifier: Parser[String] = lowercaseLetter ~ opt(ident) ^^ {
    case l ~ Some(rest) => l + rest
    case l ~ None => l
  }

  case class IdentifierRef(name: String)

  /**
    * Parse an identifier, which must not have the name of one of the given reserved words.
    *
    * @param reservedWords words which are illegal identifier names
    * @return parsed IdentifierRef
    */
  protected def identifierRef(reservedWords: Set[String], underlying: Parser[String] = ident) = new Parser[IdentifierRef] {
    def apply(in: Input): ParseResult[IdentifierRef] = {
      val pr = underlying.apply(in)
      pr match {
        case succ: Success[String @unchecked] =>
          if (reservedWords.contains(succ.get))
            Failure(s"Cannot use reserved word '${succ.get}' as function name", succ.next)
          else
            Success[IdentifierRef](IdentifierRef(succ.get), succ.next)
        case f: Failure => f
      }
    }
  }

  protected def identifierRefString(reservedWords: Set[String], underlying: Parser[String] = ident) =
    identifierRef(reservedWords, underlying) ^^ (ir => ir.name)

  // TODO to take a Seq and build a parser rather than match and check
  protected def oneOf(choices: Set[String]) = new Parser[String] {
    def apply(in: Input): ParseResult[String] = {
      val pr = ident.apply(in)
      pr match {
        case succ: Success[String @unchecked] =>
          if (!choices.contains(succ.get))
            Failure(s"Choice of '${succ.get}' not valid: Allowed were [${choices.mkString(",")}]", succ.next)
          else
            succ
        case f: Failure => f
      }
    }
  }

  case class PositionedString(s: String) extends Positional

  protected def positionedString(underlying: Parser[String]): Parser[PositionedString] = underlying ^^ (s => PositionedString(s))

  /**
    * Return a parser decorator creating a MutableTerminalTreeNode with the given name,
    * preserving position information within the total input string
    *
    * @param name       name of the field
    * @param underlying underlying parser to decorate to capture the positional information
    *                   and create the field.
    * @return new field if there is a match
    */
  protected def mutableTerminalNode(name: String, underlying: Parser[String]): Parser[MutableTerminalTreeNode] =
    positioned(positionedString(underlying)) ^^ {
      ps =>
        val inputPosition: InputPosition = ps.pos match {
          case of: OffsetPosition => new OffsetPositionInputPosition(of)
          case wtf => throw new RugRuntimeException(s"Unexpected Position type $wtf", null)
        }
        new MutableTerminalTreeNode(name, ps.s, inputPosition)
  }

  /**
    * Decorator to add position of parsed object structure.
    *
    * @param underlying parser to use
    * @param topLevel   whether this is a top level structure,
    *                   meaning we should pad after the last recognized production
    */
  def positionedStructure[T <: ParsedMutableContainerTreeNode](underlying: => Parser[T], topLevel: Boolean = false): Parser[T] = new Parser[T] {
    override def apply(in: Input): ParseResult[T] = {
      val source = in.source
      val offset = in.offset
      val start = handleWhiteSpace(source, offset)
      underlying(in) match {
        case suc: Success[T @unchecked] =>
          // Only update positions if they're not already updated
          if (suc.result.startPosition == null || suc.result.endPosition == null) {
            val inputString = in.source.toString
            suc.result.startPosition = LineHoldingOffsetInputPosition(inputString, start)
            suc.result.endPosition = LineHoldingOffsetInputPosition(inputString, suc.next.offset)
          }
          suc
        case x => x
      }
    }
  }

  /**
    * Custom exception that includes information about how many characters have
    * been consumed.
    *
    * @param message  the emessage
    * @param consumed how many characters have been consumed
    */
  protected class CustomizedParseFailureException(message: String, val consumed: Int) extends Exception(message)

  /**
    * Wrap the parser in a custom error handler that allows it throw CustomException
    * or any exception (without additional line number information)
    * and returns line number information.
    */
  def withCustomErrorHandling[T](underlying: Parser[T]): Parser[T] = new Parser[T] {
    override def apply(in: Input): ParseResult[T] = {
      val source = in.source
      val offset = in.offset
      val start = handleWhiteSpace(source, offset)
      try {
        underlying(in)
      } catch {
        case bl: CustomizedParseFailureException => Failure(bl.getMessage, in.drop(start - offset + bl.consumed))
        case e: Exception => Failure(e.getMessage, in.drop(start - offset))
      }
    }
  }

  protected def parseTo[T](f: FileArtifact, parser: Parser[T]): T = {
    logger.debug(s"Rug input is\n------\n${f.path}\n${f.content}\n------\n")
    // We need a source that gives us positions
    val source = new CharSequenceReader(f.content)
    val parsed = parse(parser, source) match {
      case Success(matched, _) => matched
      case Failure(msg, rest) =>
        throw new BadRugSyntaxException(ErrorInfo(s"Failure: $msg", badInput = f.content, line = rest.pos.line, col = rest.pos.column, filePath = f.path))
      case Error(msg, rest) =>
        throw new BadRugSyntaxException(ErrorInfo(s"Error: $msg", badInput = f.content, line = rest.pos.line, col = rest.pos.column, filePath = f.path))
    }
    logger.debug(s"Parse result=$parsed")
    parsed
  }
}

trait ToEvaluate extends Visitable

trait Literal[T] extends ToEvaluate {

  val value: T

  override def accept(v: Visitor, depth: Int): Unit = v.visit(this, depth)
}

case class SimpleLiteral[T](value: T) extends Literal[T]

/**
  * Block to evaluate in a scripting language.
  */
trait ScriptBlock extends ToEvaluate {

  def content: String
}

case class JavaScriptBlock(content: String) extends ScriptBlock {
  override def accept(v: Visitor, depth: Int): Unit = v.visit(this, depth)
}

// TODO remove this
case class GrammarBlock(content: String) extends ScriptBlock {
  override def accept(v: Visitor, depth: Int): Unit = v.visit(this, depth)

}

/**
  *
  * @param pathExpression
  * @param scalarProperty if defined, scalar property we want to extract as a String
  */
case class PathExpressionValue(
                                pathExpression: PathExpression,
                                scalarProperty: Option[String]
                              )
  extends ToEvaluate {

  override def accept(v: Visitor, depth: Int): Unit = v.visit(this, depth)

}
