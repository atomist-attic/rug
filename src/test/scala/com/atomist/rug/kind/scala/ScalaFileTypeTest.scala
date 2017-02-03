package com.atomist.rug.kind.scala

import com.atomist.rug.kind.core.ProjectMutableView
import com.atomist.source.{EmptyArtifactSource, SimpleFileBasedArtifactSource, StringFileArtifact}
import com.atomist.tree.pathexpression.{ExpressionEngine, PathExpressionEngine}
import org.scalatest.{FlatSpec, Matchers}

class ScalaFileTypeTest extends FlatSpec with Matchers {

  import ScalaFileTypeTest._

  val ee: ExpressionEngine = new PathExpressionEngine

  val scalaType = new ScalaFileType

  it should "ignore ill-formed file without error" in {
    val scalas = scalaType.findAllIn(ProjectWithBogusScala)
    // Should have silently ignored the bogus file
    scalas.size should be(1)
  }

  it should "parse hello world" in {
    val scalas = scalaType.findAllIn(HelloWorldProject)
    scalas.size should be(1)
  }
}

object ScalaFileTypeTest {

  val BogusScala = StringFileArtifact("Test.scala", "What in God's holy name are you blathering about?")

  val ProjectWithBogusScala =
    new ProjectMutableView(EmptyArtifactSource(), SimpleFileBasedArtifactSource(BogusScala))

  val HelloWorldScala = StringFileArtifact("Hello.scala",
    """
      |class Hello {
      |
      | println("Hello world")
      |}
    """.stripMargin)

  val HelloWorldProject =
    new ProjectMutableView(EmptyArtifactSource(), SimpleFileBasedArtifactSource(HelloWorldScala))

}