package com.atomist.rug.kind.elm

import scala.util.matching.Regex

object ElmTokens {

  val NewLineAtTheVeryLeft = "$" // totally imaginary. Make significant whitespace look significant to the parser
  val NewLineWithLessIndentation = "$$" // totally imaginary. Make significant whitespace look significant to the parser

  val OpenParen = "("

  val CloseParen = ")"

  val OpenCurly = "{"

  val CloseCurly = "}"

  val DotDot = ".."

  val Comma = ","

  val Colon = ":"

  val Arrow = "->"

  val ModuleKeywordToken = "module"
  val WhereKeywordToken = "where"
  val PortKeywordToken = "port"
  val ExportKeywordToken = "export"
  val ForeignKeywordToken = "foreign"
  val ExposingKeywordToken = "exposing"
  val ImportKeywordToken = "import"
  val AsKeywordToken = "as"
  val HidingKeywordToken = "hiding"
  val TypeKeywordToken = "type"
  val CaseKeywordToken = "case"
  val OfKeywordToken = "of"
  val LetKeywordToken = "let"
  val InKeywordToken = "in"
  val IfKeywordToken = "if"
  val ThenKeywordToken = "then"
  val ElseKeywordToken = "else"

  val AliasToken = "alias" // this is not a keyword

  val ReservedWords = Set(
    IfKeywordToken, ThenKeywordToken, ElseKeywordToken,
    CaseKeywordToken, OfKeywordToken,
    LetKeywordToken, InKeywordToken,
    TypeKeywordToken,
    ModuleKeywordToken, WhereKeywordToken,
    ImportKeywordToken, AsKeywordToken, HidingKeywordToken, ExposingKeywordToken,
    PortKeywordToken, ExposingKeywordToken, ForeignKeywordToken)

  val TypeIdentifier: Regex = """[A-Z][\w]*""".r

  val TypeParameterIdentifier: Regex = """[A-Za-z][\w]*""".r

  val SyntacticallyValidFunctionName: Regex = """[a-zA-Z][\w]*""".r

  // this is like, parameter names. Does not include infix function names, those are weird and we are not messing with them
  val AssignableIdentifier = """[a-z_][\w]*""".r

  val VariableName: Regex = """[a-zA-Z_][a-zA-Z0-9_']*""".r

  val LowercaseVariableName: Regex = """[a-z][a-zA-Z0-9_']*""".r
  val UppercaseVariableName: Regex = """[A-Z][a-zA-Z0-9_']*""".r
}
