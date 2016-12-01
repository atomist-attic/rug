package com.atomist.model.content.grammar.microgrammar.pattern

trait MatcherRegistry {

  def find(name: String): Option[Matcher]

  def definitions: Seq[Matcher]

}

case class SimpleMatcherRegistry(definitions: Seq[Matcher]) extends MatcherRegistry {

  override def find(name: String): Option[Matcher] =
    definitions.find(_.name.equals(name))
}

object EmptyMatcherRegistry extends MatcherRegistry {

  override def find(name: String): Option[Matcher] = None

  override def definitions: Seq[Matcher] = Nil
}