package com.atomist.rug.kind.core

import com.atomist.param.SimpleParameterValues
import com.atomist.project.edit.{ModificationAttempt, SuccessfulModification}
import com.atomist.rug.DefaultRugPipeline
import com.atomist.rug.InterpreterRugPipeline.DefaultRugArchive
import com.atomist.rug.kind.java.JavaTypeUsageTest
import com.atomist.rug.test.RugTestRunnerTestSupport
import com.atomist.source.{ArtifactSource, EmptyArtifactSource, SimpleFileBasedArtifactSource, StringFileArtifact}
import org.scalatest.{FlatSpec, Matchers}

class OverloadedMethodRunnerTest extends FlatSpec with Matchers with RugTestRunnerTestSupport {

  import com.atomist.rug.TestUtils._

  it should "It should not break when the 2 param version is called" in
    doIt(
      """
        |editor Overloader
        |
        |with Replacer r
        |   do overloaded "p1" "p2"
      """.stripMargin)

  it should "It should not break when the 1 param version is called" in
    doIt(
      """
        |editor Overloader
        |
        |with Replacer r
        |  do overloaded "p1"
      """.stripMargin)

  private def doIt(prog: String) {
    updateWith(prog, JavaTypeUsageTest.NewSpringBootProject) match {
      case nmn: SuccessfulModification =>
      case _ => ???
    }
  }

  // Return new content
  private def updateWith(prog: String, project: ArtifactSource): ModificationAttempt = {
    val pas = new SimpleFileBasedArtifactSource(DefaultRugArchive, StringFileArtifact(new DefaultRugPipeline().defaultFilenameFor(prog), prog))
    attemptModification(pas, project, EmptyArtifactSource(""), SimpleParameterValues(Map(
      "foo" -> "bar"
    )))
  }
}
