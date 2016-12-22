package com.atomist.rug

import com.atomist.project.SimpleProjectOperationArguments
import com.atomist.project.review.{ProjectReviewer, ReviewResult, Severity}
import com.atomist.rug.InterpreterRugPipeline.DefaultRugArchive
import com.atomist.rug.kind.DefaultTypeRegistry
import com.atomist.source.{ArtifactSource, SimpleFileBasedArtifactSource, StringFileArtifact}
import org.scalatest.{FlatSpec, Matchers}

class ReviewerExecutionTest extends FlatSpec with Matchers {

  import Severity._

  def ejbFinder(comment: String) =
    s"""
       |@description "Find all EJBs"
       |reviewer SpotEJBs
       |
       |with File f
       | when isJava and contains "javax.ejb"
       |do
       |  majorProblem "$comment";
      """.stripMargin

  def alwaysGripe(comment: String) =
    s"""
       |@description "Find all EJBs"
       |reviewer AlwaysGripe
       |
       |with Project p
       |do
       |minorProblem "$comment";
       """.stripMargin

  val combined =
    """
      |reviewer Combined
      |
      |SpotEJBs
      |AlwaysGripe
    """.stripMargin

  it should "find pattern when present" in {
    val as: ArtifactSource = new SimpleFileBasedArtifactSource("",
      StringFileArtifact("Thing.java",
        """
          |import javax.ejb.*;
          |public class Thing {}
        """.stripMargin))
    val comment = "EJB Alert! EJB Alert!"
    val rr = review(as, ejbFinder(comment))
    rr.comments.size should be (1)
    rr.comments.head.comment should equal(comment)
    rr.comments.head.severity should equal(MAJOR)
  }

  it should "not find pattern when not present" in {
    val as: ArtifactSource = new SimpleFileBasedArtifactSource("",
      StringFileArtifact("Thing.java",
        """
          |public class Thing {}
        """.stripMargin))
    val comment = "EJB Alert! EJB Alert!"
    val rr = review(as, ejbFinder(comment))
    rr.comments.size should be (0)
    rr.severity should be (FINE)
  }

  it should "execute two reviewers" in {
    val as: ArtifactSource = new SimpleFileBasedArtifactSource("",
      StringFileArtifact("Thing.java",
        """
          |import javax.ejb.*;
          |public class Thing {}
        """.stripMargin))
    val comment = "EJB Alert! EJB Alert!"
    val comment2 = "Needs Juergenization"
    val prog = combined + "\n" + alwaysGripe(comment2) + "\n" + ejbFinder(comment)
    val rr = review(as, prog)
    rr.comments.size should be(2)
    rr.comments.head.comment should equal(comment)
    rr.comments.head.severity should equal(MAJOR)
    rr.comments(1).comment should equal(comment2)
    rr.comments(1).severity should equal(POLISH)
  }

  private def review(as: ArtifactSource, prog: String): ReviewResult = {
    val runtime = new DefaultRugPipeline(DefaultTypeRegistry)
    val eds = runtime.createFromString(prog)
    val pe = eds.head.asInstanceOf[ProjectReviewer]
    pe.review(as, SimpleProjectOperationArguments.Empty)
  }

}
