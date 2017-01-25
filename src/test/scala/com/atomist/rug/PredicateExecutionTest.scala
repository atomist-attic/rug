package com.atomist.rug

import com.atomist.param.SimpleParameterValues
import com.atomist.rug.kind.DefaultTypeRegistry
import com.atomist.rug.runtime.rugdsl.RugDrivenProjectPredicate
import com.atomist.source.{ArtifactSource, SimpleFileBasedArtifactSource, StringFileArtifact}
import org.scalatest.{FlatSpec, Matchers}

class PredicateExecutionTest extends FlatSpec with Matchers {

  val ejbFinder =
    s"""
       |predicate UsesEJBs
       |
       |with File f
       | when isJava and contains "javax.ejb"
      """.stripMargin

  val drNo =
    s"""
       |@description "My name is No, my number is No"
       |predicate DrNo
       |
       |with Project p
       """.stripMargin

  val alwaysHappy =
    s"""
       |predicate AlwaysHappy
       |
       |with Project when false
       """.stripMargin

  //  val combined =
  //    """
  //      |reviewer Combined
  //      |
  //      |SpotEJBs
  //      |AlwaysGripe
  //    """.stripMargin

  it should "not match" in {
    val as: ArtifactSource = new SimpleFileBasedArtifactSource("",
      StringFileArtifact("Thing.java",
        """
          |import javax.ejb.*;
          |public class Thing {}
        """.stripMargin))
    val rr = runPredicate(as, alwaysHappy)
    rr should be(false)
  }

  it should "find pattern when present" in {
    val as: ArtifactSource = new SimpleFileBasedArtifactSource("",
      StringFileArtifact("Thing.java",
        """
          |import javax.ejb.*;
          |public class Thing {}
        """.stripMargin))
    val rr = runPredicate(as, ejbFinder)
    rr should be(true)
  }

  private def runPredicate(as: ArtifactSource, prog: String): Boolean = {
    val runtime = new DefaultRugPipeline(DefaultTypeRegistry)
    val eds = runtime.createFromString(prog)
    val pe = eds.head.asInstanceOf[RugDrivenProjectPredicate]
    pe.holds(as, SimpleParameterValues.Empty)
  }
}