package com.atomist.tree.content.text.microgrammar.dsl

import com.atomist.graph.GraphNode
import com.atomist.parse.java.ParsingTargets
import com.atomist.project.archive.DefaultAtomistConfig
import com.atomist.rug.kind.DefaultTypeRegistry
import com.atomist.rug.kind.core.ProjectMutableView
import com.atomist.rug.spi.UsageSpecificTypeRegistry
import com.atomist.source.EmptyArtifactSource
import com.atomist.tree.content.text.microgrammar.{MatcherMicrogrammar, MicrogrammarTypeProvider}
import com.atomist.tree.pathexpression.{ExpressionEngine, PathExpressionEngine}
import com.atomist.tree.{MutableTreeNode, TreeNode, UpdatableTreeNode}
import org.scalatest.{FlatSpec, Matchers}

/**
  * Test that path expressions can use microgrammars
  */
class MicrogrammarUsageInPathExpressionTest extends FlatSpec with Matchers {

  // Import for implicit conversion from String to PathExpression
  import com.atomist.tree.pathexpression.PathExpressionParser._

  val ee: ExpressionEngine = new PathExpressionEngine

  val mgp = new MatcherDefinitionParser

  it should "use simple microgrammar to match in single file" in
    useSimpleMicrogrammarAgainstSingleFile

  it should "use simple microgrammar in a single file and modify content" in {
    val (pmv, nodes) = useSimpleMicrogrammarAgainstSingleFile
    val highlyImprobableValue = "woieurowiuroepqirupoqwieur"
    assert(nodes.size === 1)
    withClue(s"Type was ${nodes.head.nodeTags}") {
      nodes.head.nodeTags.contains("modelVersion") should be(true)
    }
    nodes.head match {
      case mtn: UpdatableTreeNode =>
        mtn.update(highlyImprobableValue)
        val newContent = pmv.findFile("pom.xml").content
        newContent.contains(highlyImprobableValue) should be(true)
      case x =>
        fail(s"What is this? $x")

    }
  }

  // Return the project and matched nodes
  private  def useSimpleMicrogrammarAgainstSingleFile: (ProjectMutableView, Seq[GraphNode]) = {
    val proj = ParsingTargets.NewStartSpringIoProject
    val pmv = new ProjectMutableView(EmptyArtifactSource(""), proj, DefaultAtomistConfig)
    val findFile = "/File()[@name='pom.xml']"

    val mg: MatcherMicrogrammar = new MatcherMicrogrammar(
      mgp.parseMatcher("pom",
        "<modelVersion>$modelVersion:§[a-zA-Z0-9_\\.]+§</modelVersion>"), "pom")

    val matches = mg.findMatches(proj.findFile("pom.xml").get.content)
    assert(matches.length === 1)

    val tr = new UsageSpecificTypeRegistry(DefaultTypeRegistry,
      Seq(new MicrogrammarTypeProvider(mg))
    )
    val rtn = ee.evaluate(pmv, findFile, tr)
    assert(rtn.right.get.size === 1)

    val modelVersion = findFile + "/pom()/modelVersion()"

    val grtn = ee.evaluate(pmv, modelVersion, tr)
    assert(grtn.right.get.size === 1)
    (pmv, grtn.right.get)
  }

}
