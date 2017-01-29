package com.atomist.tree.content.text.microgrammar

import com.atomist.tree.content.text.microgrammar.dsl.MatcherDefinitionParser


object MatcherMicrogrammarConstruction {

  private val matcherParser = new MatcherDefinitionParser()

  // I feel like this should return an Either. will wait for a use case
  def matcherMicrogrammar(name: String, grammar: String, submatchers: Map[String, Any] = Map()): MatcherMicrogrammar = {

    val parsedMatcher = matcherParser.parseMatcher(name, grammar)

    val knownMatchers: Map[String, Matcher] = submatchers.map {
      case (name, grammar : String) => (name, matcherParser.parseMatcher(name, grammar))
    }

    new MatcherMicrogrammar(parsedMatcher, name, knownMatchers)
  }

}
