package com.atomist.tree.content.text.microgrammar

import com.atomist.parse.java.ParsingTargets
import com.atomist.project.archive.DefaultAtomistConfig
import com.atomist.rug.kind.DefaultTypeRegistry
import com.atomist.rug.kind.core.ProjectMutableView
import com.atomist.rug.kind.dynamic.MutableContainerTreeNodeMutableView
import com.atomist.rug.spi.UsageSpecificTypeRegistry
import com.atomist.source.EmptyArtifactSource
import com.atomist.tree.{MutableTreeNode, TreeNode}
import com.atomist.tree.pathexpression.{ExpressionEngine, PathExpressionEngine}
import org.scalatest.{FlatSpec, Matchers}

/**
  * Test that path expressions can use microgrammars
  */
class MicrogrammarUsageInPathExpressionTest extends FlatSpec with Matchers {

  // Import for implicit conversion from String to PathExpression
  import com.atomist.tree.pathexpression.PathExpressionParser._

  val ee: ExpressionEngine = new PathExpressionEngine

  val mgp = new MatcherDSLDefinitionParser

  it should "use simple microgrammar to match in single file" in
    useSimpleMicrogrammarAgainstSingleFile

  it should "use simple microgrammar in a single file and modify content" in {
    val (pmv, nodes) = useSimpleMicrogrammarAgainstSingleFile
    val highlyImprobableValue = "woieurowiuroepqirupoqwieur"
    nodes.size should be (1)
    nodes.head match {
      case mtn: MutableContainerTreeNodeMutableView =>
        mtn.update(highlyImprobableValue)
        val newContent = pmv.findFile("pom.xml").content
        //println(s"New content=\n$newContent")
        newContent.contains(highlyImprobableValue) should be(true)
    }
  }

  // Return the project and matched nodes
  private def useSimpleMicrogrammarAgainstSingleFile: (ProjectMutableView, Seq[TreeNode]) = {
    val proj = ParsingTargets.NewStartSpringIoProject
    val pmv = new ProjectMutableView(EmptyArtifactSource(""), proj, DefaultAtomistConfig)
    // TODO should we insist on a starting axis specifier for consistency?
    val findFile = "/*:File[name='pom.xml']"
    val mg: Microgrammar = new MatcherMicrogrammar("modelVersion",
      mgp.parse("<modelVersion>$modelVersion:§[a-zA-Z0-9_\\.]+§</modelVersion>"))

    val tr = new UsageSpecificTypeRegistry(DefaultTypeRegistry,
      Seq(new MicrogrammarTypeProvider(mg))
    )
    val rtn = ee.evaluate(pmv, findFile, tr)
    rtn.right.get.size should be(1)
    // TODO do we need the name
    val modelVersion = findFile + "->modelVersion"
    val grtn = ee.evaluate(pmv, modelVersion, tr)
    grtn.right.get.size should be(1)
    (pmv, grtn.right.get)
  }

}
