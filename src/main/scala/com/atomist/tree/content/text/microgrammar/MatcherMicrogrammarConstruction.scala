package com.atomist.tree.content.text.microgrammar

import com.atomist.tree.content.text.microgrammar.dsl.MatcherDefinitionParser
import com.atomist.tree.content.text.microgrammar.matchers.Break

object MatcherMicrogrammarConstruction {

  private val matcherParser = new MatcherDefinitionParser()

  // I feel like this should return an Either. will wait for a use case
  def matcherMicrogrammar(name: String, grammar: String, submatchers: Map[String, Object] = Map()): MatcherMicrogrammar = {
    val parsedMatcher = matcherParser.parseMatcher(name, grammar)

    val knownMatchers: Map[String, Matcher] = submatchers.map {
      case (name, matcher) => (name, interpretMatcher(matcher, name))
    }

    new MatcherMicrogrammar(parsedMatcher, name, knownMatchers)
  }

  def constructOr(properties: Map[String, Any]): Matcher = {
    val componentProperty = properties.getOrElse("components", throw new RuntimeException("an Or should have components"))
    // I have no idea why the array comes in as a map of indices to values but it does
    val components = jsArrayToList(componentProperty).map(interpretAnonymousMatcher)
    components.reduce(_.alternate(_))
  }

  private def jsArrayToList(a: Any) =
    a match {
      case m: Map[_, _] => m.toList.sortBy(
        {case (n: Number, _) => n.intValue()
        case (s: String, _) => s.toInt // why, Nashorn, why??
        case (wat, _) => throw new RuntimeException(s"This is a ${wat.getClass}: ${wat}")}
      ).map(_._2)
      case _ => throw new RuntimeException("expected an array, as a Map[Number, Any]")
    }


  def constructConcat(properties: Map[String, Any]): Matcher = {
    val componentProperty = properties.getOrElse("components", throw new RuntimeException("a Concat should have components"))
    // I have no idea why the array comes in as a map of indices to values but it does
    val components = jsArrayToList(componentProperty).map(interpretAnonymousMatcher)
    components.reduce(_.concat(_))
  }

  def constructRegex(properties: Map[String, Any]): Matcher = {
    val expressionProperty = properties.getOrElse("regularExpression", throw new RuntimeException("a Regex should have a regularExpression"))
    val e = expressionProperty match {
      case m: String => m
      case _ => throw new RuntimeException("expected a string for regularExpression")
    }
    Regex(e)
  }

  def constructRepeat(properties: Map[String, Any]): Matcher = {
    val expressionProperty = properties.getOrElse("what", throw new RuntimeException("a Repeat should have a what"))
    val e = interpretAnonymousMatcher(expressionProperty)
    Rep(e)
  }

  def constructOptional(properties: Map[String, Any]): Matcher = {
    val expressionProperty = properties.getOrElse("what", throw new RuntimeException("an Optional should have a what"))
    val e = interpretAnonymousMatcher(expressionProperty)
    Optional(e)
  }

  def constructBreak(properties: Map[String, Any]): Matcher = {
    val expressionProperty = properties.getOrElse("to", throw new RuntimeException("a Break should have a to"))
    val e = interpretAnonymousMatcher(expressionProperty)
    Break(e)
  }

  def constructStrictLiteral(properties: Map[String, Any]): Matcher = {
    val expressionProperty = properties.getOrElse("content", throw new RuntimeException("a strict literal should have a content"))
    val e = expressionProperty match {
      case m: String => m
      case _ => throw new RuntimeException("expected a string for content")
    }
    Literal(e)
  }

  def interpretAnonymousMatcher(m: Any): Matcher = m match {
    case grammar: String => matcherParser.parseAnonymous(grammar)
    case micromatcher: Map[String, Any]@unchecked  => parseMicromatcher(micromatcher.asInstanceOf[Map[String, Any]])
    case _ => throw new RuntimeException(s"Unrecognized object provided for a matcher ${m}")
  }

  def parseMicromatcher(js: Map[String, Any]): Matcher =
     js("kind") match {
       case "or" => constructOr(js)
       case "regex" => constructRegex(js)
       case "repeat" => constructRepeat(js)
       case "optional" => constructOptional(js)
       case "strict-literal" => constructStrictLiteral(js)
       case "concat" => constructConcat(js)
       case "break" => constructBreak(js)
       case k => throw new RuntimeException(s"Unrecognized kind of matcher: ${k}")
     }

  def interpretMatcher(m: Any, name: String): Matcher = m match {
    case grammar: String => matcherParser.parseMatcher(name, grammar)
    case micromatcher: Map[String, Any]@unchecked  => parseMicromatcher(micromatcher.asInstanceOf[Map[String, Any]])
    case _ => throw new RuntimeException(s"Unrecognized object provided for submatcher called $name")
  }

}
