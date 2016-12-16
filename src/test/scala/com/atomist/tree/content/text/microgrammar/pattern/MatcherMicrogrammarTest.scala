package com.atomist.tree.content.text.microgrammar.pattern

import com.atomist.tree.content.text.microgrammar.{Microgrammar, _}
import com.atomist.rug.kind.grammar.MicrogrammarTest

class MatcherMicrogrammarTest extends MicrogrammarTest {

  import Literal._

  override protected def thingGrammar: Microgrammar = {
    val matcher = Regex("thing", ".*")
    new MatcherMicrogrammar(matcher)
  }

  override protected def matchPrivateJavaFields: Microgrammar = {
    val field = "private" ~~ Regex("type", "[a-zA-Z0-9]+") ~~ Regex("name", "[a-zA-Z0-9]+")
    new MatcherMicrogrammar(field)
  }

  override protected def aWasaB: Microgrammar =
    new MatcherMicrogrammar(
      Regex("name", "[A-Z][a-z]+") ~? Literal("was aged") ~? Regex("age", "[0-9]+")
    )


//  IDENTIFIER : [a-zA-Z0-9]+;
//  LPAREN : '(';
//  RPAREN : ')';
//  param_def : name=IDENTIFIER ':' type=IDENTIFIER;
//  params : param_def (',' param_def)*;
//  method : 'def' name=IDENTIFIER LPAREN params? RPAREN ':' type=IDENTIFIER;
  override protected def matchScalaMethodHeaderRepsep: Microgrammar = {
    val identifier = Regex("identifier", "[a-zA-Z0-9]+")
    val paramDef = Wrap(
      identifier.copy(name = "name") ~? ":" ~? identifier.copy(name = "type"),
      "param_def")
    val params = Repsep(paramDef, ",", "params")
    val method = "def" ~~ identifier.copy(name = "name") ~? "(" ~?
      Wrap(params, "params") ~? ")" ~? ":" ~? identifier.copy(name = "type")
    new MatcherMicrogrammar(method)
  }

  override protected def matchAnnotatedJavaFields: Microgrammar = {
    val visibility: Matcher = "public" | "private"
    val annotation = "@" ~ Regex("annotationType", "[a-zA-Z0-9]+")
    val field = annotation ~~ visibility ~~ Regex("type", "[a-zA-Z0-9]+") ~~ Regex("name", "[a-zA-Z0-9]+")
    new MatcherMicrogrammar(field)
  }

  //  KEY: [A-Za-z_]+;
  //  VALUE: [A-Za-z0-9\-]+;
  //  keys: '-' KEY (':' | '=') VALUE ;
  //  env_list: 'env:' 'global:' key=keys*;
  override protected def ymlKeys: Microgrammar = {
    val key: Matcher = Regex("key", "[A-Za-z_]+")
    val value = Regex("value", "[A-Za-z0-9\\-]+")
    val pair = "-" ~? key ~? Alternate(":", "=") ~? value
    val envList = "env:" ~~ "global:" ~~ Repsep(pair, WhitespaceOrNewLine, "keys")
    new MatcherMicrogrammar(envList)
  }

  override protected def repTest: Microgrammar = {
    val key: Matcher = Regex("key", "[A-Za-z_]+,")
    val sentence: Matcher = Literal("keys:", Some("prefix")) ~? Wrap(Rep(key, "keys"), "keys")
    new MatcherMicrogrammar(sentence)
  }

  override protected def repsepTest: Microgrammar = {
    val key: Matcher = Regex("key", "[A-Za-z_]+")
    val sentence: Matcher = Literal("keys:", Some("prefix")) ~? Wrap(Repsep(key, ",", "keys"), "keys")
    new MatcherMicrogrammar(sentence)
  }

  override protected def printlns: Microgrammar = {
    // Yes this is naive because of linebreaks and escaped )
    val printlns = "println(" ~ Break(")", Some("content"))
    new MatcherMicrogrammar(printlns)
  }

}
