package com.atomist.rug.kind.core

import com.atomist.parse.java.ParsingTargets._
import com.atomist.tree.pathexpression.PathExpressionEngine
import org.scalatest.{FlatSpec, Matchers}

class DirectoryMutableViewTest extends FlatSpec with Matchers {

  import com.atomist.tree.pathexpression.PathExpressionParser._

  val peng = new PathExpressionEngine

  "DirectoryMutableView" should "be resolved in a path expression" in {
    val pmv = new ProjectMutableView(NewStartSpringIoProject)
    val expr = "/src/main/java"
    peng.evaluate(pmv, expr) match {
      case Right(nodes) =>
        assert(nodes.size === 1)
        val dmv = nodes.head.asInstanceOf[DirectoryMutableView]
        assert(dmv.path === "src/main/java")
        assert(dmv.totalFileCount > 0)
        assert(dmv.fileCount === 0)
      case x => fail(s"Unexpected: $x")
    }
  }

}
