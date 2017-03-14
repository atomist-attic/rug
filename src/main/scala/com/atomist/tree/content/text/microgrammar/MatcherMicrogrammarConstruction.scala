package com.atomist.tree.content.text.microgrammar

import com.atomist.tree.content.text.microgrammar.dsl.MatcherDefinitionParser

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
    val componentProperty = properties.getOrElse("components", throw new RuntimeException("an Or should have a component"))
    // I have no idea why the array comes in as a map of indices to values but it does
    val components = componentProperty match {
      case m: Map[_, _] => m.values.map(interpretMatcher(_))
      case _ => throw new RuntimeException("expected an array of 'or' components, as a Map[Number, Any]")
    }
    components.reduce(_.alternate(_))
  }

  def interpretMatcher(m: Any, name: String = "anonymous") = m match {
    case grammar: String => matcherParser.parseMatcher(name, grammar)
    case micromatcher: Map[String, Any] @unchecked if micromatcher("kind") == "or" =>
      constructOr(micromatcher.asInstanceOf[Map[String, Any]])
    case _ => throw new RuntimeException(s"Unrecognized object provided for submatcher $name")

  }

}
