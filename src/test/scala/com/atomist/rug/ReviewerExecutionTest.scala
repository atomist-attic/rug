package com.atomist.rug

import com.atomist.param.SimpleParameterValues
import com.atomist.project.review.{ProjectReviewer, ReviewResult, Severity}
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
    assert(rr.comments.size === 1)
    assert(rr.comments.head.comment === comment)
    assert(rr.comments.head.severity === MAJOR)
  }

  it should "not find pattern when not present" in {
    val as: ArtifactSource = new SimpleFileBasedArtifactSource("",
      StringFileArtifact("Thing.java",
        """
          |public class Thing {}
        """.stripMargin))
    val comment = "EJB Alert! EJB Alert!"
    val rr = review(as, ejbFinder(comment))
    assert(rr.comments.size === 0)
    assert(rr.severity === FINE)
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
    assert(rr.comments.size === 2)
    assert(rr.comments.head.comment === comment)
    assert(rr.comments.head.severity === MAJOR)
    assert(rr.comments(1).comment === comment2)
    assert(rr.comments(1).severity === POLISH)
  }

  private  def review(as: ArtifactSource, prog: String): ReviewResult = {
    val runtime = new DefaultRugPipeline(DefaultTypeRegistry)
    val eds = runtime.createFromString(prog)
    val pe = eds.head.asInstanceOf[ProjectReviewer]
    pe.review(as, SimpleParameterValues.Empty)
  }

}
