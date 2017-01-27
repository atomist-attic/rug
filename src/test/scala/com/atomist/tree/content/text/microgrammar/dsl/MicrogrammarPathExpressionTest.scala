package com.atomist.tree.content.text.microgrammar.dsl

import com.atomist.project.archive.DefaultAtomistConfig
import com.atomist.rug.kind.DefaultTypeRegistry
import com.atomist.rug.kind.core.ProjectMutableView
import com.atomist.rug.spi.UsageSpecificTypeRegistry
import com.atomist.source.{SimpleFileBasedArtifactSource, StringFileArtifact}
import com.atomist.tree.TreeNode
import com.atomist.tree.content.text.microgrammar._
import com.atomist.tree.pathexpression.{PathExpressionEngine, PathExpressionParser}
import org.scalatest.{FlatSpec, Matchers}


class OptionalFieldMicrogrammarTest extends FlatSpec with Matchers {
  it should "Let me access something that might exist but does not" in pending


  def exercisePathExpression(microgrammar: Microgrammar, pathExpressionString: String, input: String): List[TreeNode] = {

    /* Construct a root node */
    val as = SimpleFileBasedArtifactSource(StringFileArtifact("banana.txt", input))
    val pmv = new ProjectMutableView(as /* cheating */, as, DefaultAtomistConfig)

    /* Parse the path expression */
    val pathExpression = PathExpressionParser.parseString(pathExpressionString)

    /* Install the microgrammar */
    val typeRegistryWithMicrogrammar =
      new UsageSpecificTypeRegistry(DefaultTypeRegistry,
        Seq(new MicrogrammarTypeProvider(microgrammar)))

    val result = new PathExpressionEngine().evaluate(pmv, pathExpression, typeRegistryWithMicrogrammar)

    result match {
      case Left(a) => fail(a)
      case Right(b) => b
    }
  }

  val input: String =
    """There was a banana. It crossed the street. A car ran over it.
      |No banana for you.
      |""".stripMargin

  it should "match an unnamed literal" in {

    val microgrammar = new MatcherMicrogrammar(Literal("banana"), "bananagrammar")
    val pathExpression = "/File()/bananagrammar()"

    val result = exercisePathExpression(microgrammar, pathExpression, input)

    result.size should be(2)
  }
}