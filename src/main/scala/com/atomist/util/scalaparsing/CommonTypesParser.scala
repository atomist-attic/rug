package com.atomist.util.scalaparsing

import com.atomist.rug.{BadRugException, BadRugSyntaxException, RugRuntimeException}
import com.atomist.source.FileArtifact
import com.atomist.tree.content.text._
import com.typesafe.scalalogging.LazyLogging

import scala.util.matching.Regex
import scala.util.parsing.combinator.JavaTokenParsers
import scala.util.parsing.input.{CharSequenceReader, OffsetPosition, Positional}

/**
  * Convenient support for parsers for different languages implemented using
  * Scala parser combinators.
  * Offers common tokens, consistent reserved word handling, comment and string handling,
  * and productions for nested blocks.
  *
  * @see TreeNode
  */
abstract class CommonTypesParser extends JavaTokenParsers with LazyLogging {

  val HashLineComment = """\#.*[\n|\r\n]"""

  // NB: This does not correctly preserve positions
  def doubleQuotedString: Parser[String] = (stringLiteral | ("\"" ~ "[^\"]+".r ~ "\"")) ^^ {
    case s : String => s.substring(1).dropRight(1).replace("""\\""", """\""")
    case _ ~ illFormedString ~ _ => throw new BadRugException(
      s"It looks like you're trying to use a string, but [$illFormedString] is not a valid Java String") {}
  }

  val EqualsToken = "="

  def singleQuote = "'"

  def singleQuotedString: Parser[String] = singleQuote ~> """[^']*""".r <~ singleQuote

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
        case _ => ???
      }
    }
  }

  protected def identifierRefString(reservedWords: Set[String], underlying: Parser[String] = ident): Parser[String] =
    identifierRef(reservedWords, underlying) ^^ (ir => ir.name)

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
