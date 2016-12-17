package com.atomist.tree.content.text.microgrammar

import com.atomist.parse.java.ParsingTargets
import com.atomist.project.archive.DefaultAtomistConfig
import com.atomist.rug.kind.core.ProjectMutableView
import com.atomist.source.EmptyArtifactSource
import com.atomist.tree.pathexpression.{ExpressionEngine, PathExpressionEngine}
import org.scalatest.{FlatSpec, Matchers}

/**
  * Test that path expressions can use microgrammars
  */
class MicrogrammarUsageInPathExpressionTest extends FlatSpec with Matchers {

  import com.atomist.tree.pathexpression.PathExpressionParser._

  val ee: ExpressionEngine = new PathExpressionEngine

  val mgp = new MatcherDSLDefinitionParser

  it should "use simple microgrammar against single file" in pendingUntilFixed {
    val proj = ParsingTargets.NewStartSpringIoProject
    val pmv = new ProjectMutableView(EmptyArtifactSource(""), proj, DefaultAtomistConfig)
    // TODO should we insist on a starting axis specifier for consistency?
    val findFile = "/*:file[name='pom.xml']"
    val rtn = ee.evaluate(pmv, findFile)
    rtn.right.get.size should be(1)
    val mg: Microgrammar = new MatcherMicrogrammar(mgp.parse("<groupId>$groupId:§[a-zA-Z0-9_]+§</groupId>"))
    // TODO do we need the name
    val findGroupId = findFile + "->gid"
    val grtn = ee.evaluate(pmv, findGroupId)
    grtn.right.get.size should be(1)
  }

}
