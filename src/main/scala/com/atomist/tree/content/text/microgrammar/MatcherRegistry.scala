package com.atomist.tree.content.text.microgrammar

/**
  * Registry of the matchers we know about
  */
trait MatcherRegistry {

  def find(name: String): Option[Matcher]

  def definitions: Seq[Matcher]

  def +(m: Matcher) = SimpleMatcherRegistry(definitions :+ m)

  override def toString = s"${getClass.getSimpleName}: Matchers=[${definitions.map(_.name).mkString(",")}]"

}

case class SimpleMatcherRegistry(definitions: Seq[Matcher]) extends MatcherRegistry {

  override def find(name: String): Option[Matcher] =
    definitions.find(_.name.equals(name))
}

object EmptyMatcherRegistry extends SimpleMatcherRegistry(Nil)