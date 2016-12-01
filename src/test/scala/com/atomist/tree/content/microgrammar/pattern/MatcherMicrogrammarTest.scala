package com.atomist.tree.content.microgrammar.pattern

import com.atomist.tree.content.microgrammar._
import com.atomist.rug.kind.grammar.MicrogrammarTest

abstract class MatcherMicrogrammarTest extends MicrogrammarTest {

  import Literal._

  override protected def thingGrammar: Microgrammar = {
    val matcher = Regex("thing", ".*")
    new MatcherMicrogrammar(matcher)
  }

  override protected def matchPrivateJavaFields: Microgrammar = ???

  override protected def aWasaB: Microgrammar =
    new MatcherMicrogrammar(
      Regex("name", "[A-Z][a-z]+") ~? Literal("was aged") ~? Regex("age", "[0-9]+")
    )

  //
  """
    | IDENTIFIER : [a-zA-Z0-9]+;
    | LPAREN : '(';
    | RPAREN : ')';
    | param_def : name=IDENTIFIER ':' type=IDENTIFIER;
    | params : param_def (',' param_def)*;
    | method : 'def' name=IDENTIFIER LPAREN params? RPAREN ':' type=IDENTIFIER;
  """.stripMargin

  //
  override protected def matchScalaMethodHeaderRepsep: Microgrammar = {
    val identifier = Regex("identifier", "[a-zA-Z0-9]+")
    val paramDef = identifier.copy(name = "name") ~? ":" ~? identifier.copy(name = "type")
    val params = Rep(paramDef)
    val method = "def" ~~ identifier.copy(name = "name") ~? "(" ~? params ~? ")" ~? ":" ~? identifier.copy(name = "type")
    new MatcherMicrogrammar(method)
  }

  override protected def matchAnnotatedJavaFields: Microgrammar = {
    val visibility: Matcher = "public" | "private"
    val annotation = "@" ~ Regex("annotationType", "[a-zA-Z0-9]+")
    val field = annotation ~~ visibility ~~ Regex("type", "[a-zA-Z0-9]+") ~~ Regex("name", "[a-zA-Z0-9]+")
    new MatcherMicrogrammar(field)
  }

  override protected def ymlKeys: Microgrammar = ???
}
