package com.atomist.tree.content.text.microgrammar.dsl

import com.atomist.project.archive.DefaultAtomistConfig
import com.atomist.rug.kind.DefaultTypeRegistry
import com.atomist.rug.kind.core.ProjectMutableView
import com.atomist.rug.spi.UsageSpecificTypeRegistry
import com.atomist.source.{SimpleFileBasedArtifactSource, StringFileArtifact}
import com.atomist.tree.content.text.microgrammar.{Literal, MatcherMicrogrammar, MicrogrammarTypeProvider}
import com.atomist.tree.pathexpression.{PathExpressionEngine, PathExpressionParser}
import org.scalatest.{FlatSpec, Matchers}


class OptionalFieldMicrogrammarTest extends FlatSpec with Matchers {
  it should "Let me access something that might exist but does not" in pending

  it should "match an unnamed literal" in {
    val input =
      """There was a banana. It crossed the street. A car ran over it.
        |No banana for you.
        |""".stripMargin
    val as = SimpleFileBasedArtifactSource(StringFileArtifact("banana.txt", input))
    val pmv = new ProjectMutableView(as, as, DefaultAtomistConfig)

    val microgrammar =
      new MatcherMicrogrammar(
        Literal("banana")  // another test needs: Literal("a ) ~ Regex("[a-z]+", "blah") ~ Optional(Literal("yo", Some("myWord"))) ~ Literal(".")
        , "bananagrammar")  
    val pathExpression = PathExpressionParser.parseString("/File()/bananagrammar()")

    val typeRegistryWithMicrogrammar =
      new UsageSpecificTypeRegistry(DefaultTypeRegistry,
        Seq(new MicrogrammarTypeProvider(microgrammar)))

    val result = new PathExpressionEngine().evaluate(pmv, pathExpression, typeRegistryWithMicrogrammar)

    result match {
      case Left(a) => fail(a)
      case Right(b) => b.size should be(2)
    }

  }
}