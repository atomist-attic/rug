package com.atomist.rug

import com.atomist.project.SimpleProjectOperationArguments
import com.atomist.project.edit._
import com.atomist.rug.kind.DefaultTypeRegistry
import com.atomist.source.{SimpleFileBasedArtifactSource, StringFileArtifact}
import org.scalatest.{FlatSpec, Matchers}

class ConditionsTest extends FlatSpec with Matchers {

  val simpleAs = new SimpleFileBasedArtifactSource("name",
    Seq(
      StringFileArtifact("filename", "some content")
    )
  )

  it should "use valid precondition in same file using reviewer" in
    validPreconditionInSameFile(
      """
        |reviewer AlwaysGripe
        |with project p
        |do
        |minorProblem "I'm a PITA"
      """.stripMargin)

  it should "use valid precondition in same file using predicate" in
    validPreconditionInSameFile(
      """
        |# Note that this doesn't ever gripe, but we are using the same name
        |predicate AlwaysGripe
        |with project when false
      """.stripMargin)

  private def validPreconditionInSameFile(alwaysGripePrecondition: String) {
    def prog(guard: Boolean) =
      s"""
         |editor Redeploy
         |
         |${if (guard) "precondition AlwaysGripe" else ""}
         |
         |with file f
         |do
         |  replace "some" "foo"
         |
         |Foo
         |
         |editor Foo
         |with file
         |do replace "content" "bar"
         |
         |$alwaysGripePrecondition
      """.stripMargin
    val unguarded = create(prog(false))
    unguarded.applicability(simpleAs).canApply should be(true)
    unguarded.modify(simpleAs, SimpleProjectOperationArguments.Empty)
    match {
      case sm: SuccessfulModification =>
    }
    val guarded = create(prog(true))
    guarded.applicability(simpleAs).canApply should be(false)
    guarded.modify(simpleAs, SimpleProjectOperationArguments.Empty)
    match {
      case nmn: NoModificationNeeded =>
    }
  }

  it should "handle nested precondition" in {
    val prog =
      s"""
         |editor Redeploy
         |
         |precondition Maybe
         |precondition AlwaysTrue
         |
         |with file f
         |do
         |  replace "some" "foo"
         |
         |predicate AlwaysTrue
         |with project
      """.stripMargin

    val truePred1 =
      """
        |predicate Maybe
        |with file when name = "filename"
      """.stripMargin

    val truePred2 =
      """
        |predicate Maybe
        |with project
        | with file when name = "filename"
      """.stripMargin

    val falsePred1 =
      """
        |predicate Maybe
        |with file when name = "certainlyNotThis"
      """.stripMargin

    val falsePred2 =
      """
        |predicate Maybe
        |# This won't match as it's not a Spring boot project
        |with spring.bootProject
      """.stripMargin

    val falsePred3 =
      """
        |predicate Maybe
        |# This won't match as it's not a Spring boot project
        |with java.project
      """.stripMargin

    val shouldDoIts = Seq(truePred1, truePred2).map(pred => prog + "\n\n" + pred)
    val shouldNotDoIts = Seq(falsePred1, falsePred2, falsePred3).map(pred => prog + "\n\n" + pred)

    for (doit <- shouldDoIts) {
      val ja = create(doit)
      ja.applicability(simpleAs).canApply should be(true)
      ja.modify(simpleAs, SimpleProjectOperationArguments.Empty)
      match {
        case sm: SuccessfulModification =>
      }
    }

    for (dont <- shouldNotDoIts) {
      val nein = create(dont)
      nein.applicability(simpleAs).canApply should be(false)
      nein.modify(simpleAs, SimpleProjectOperationArguments.Empty)
      match {
        case nmn: NoModificationNeeded =>
      }
    }
  }

  it should "ignore always false valid precondition in same file" in {
    val prog =
      """
        |editor Redeploy
        |
        |precondition NeverGripe
        |
        |with file f
        |do
        |  replace "some" "foo"
        |
        |Foo
        |
        |editor Foo
        |
        |with file f
        |do replace "content" "bar"
        |
        |
        |reviewer NeverGripe
        |with file f
        |when "a" = "b"
        |do
        |minorProblem "I'm a PITA"
      """.stripMargin
    val harmlessGuard = create(prog)
    harmlessGuard.applicability(simpleAs).canApply should be(true)
    harmlessGuard.modify(simpleAs, SimpleProjectOperationArguments.Empty)
  }

  it should "reject missing precondition" in {
    val prog =
      """
        |editor Redeploy
        |
        |precondition NeverGripe
        |
        |with file f
        |do
        |  replace "some" "foo"
        |Foo
        |
        |editor Foo
        |
        |with file f
        |do replace "content" "bar"
      """.stripMargin
    an[UndefinedRugUsesException] should be thrownBy create(prog)
  }

  it should "reject precondition with parameters" in {
    val prog =
      """
        |editor Redeploy
        |
        |precondition NeverGripe
        |
        |with file f
        |do
        |  replace "some" "foo"
        |Foo
        |
        |editor Foo
        |
        |with file f
        |do replace "content" "bar"
        |
        |
        |reviewer NeverGripe
        |
        |param whatever: .*
        |
        |with file f
        |when "a" = "b"
        |do
        |minorProblem "I'm a PITA"
        |
      """.stripMargin
    an[InvalidRugUsesException] should be thrownBy create(prog)
  }

  private def postconditionProg(postcondition: String) =
    s"""
       |editor Redeploy
       |
       |$postcondition
       |
       |with file f
       |do
       |  replace "some" "foo"
       |
       |Foo
       |
       |editor Foo
       |
       |with file f
       |do replace "content" "bar"
       |
       |reviewer AlwaysGripe
       |
       |with project p
       |do
       |minorProblem "I'm a PITA"
       |
       |reviewer NeverGripes
       |with file f when "a" = "b"
       |do minorProblem "I never get invoked"
       |
       |reviewer DidIt
       |
       |with file f
       |when { !f.content().contains("bar") }
       |do majorProblem "didn't run"
      """.stripMargin

  it should "use no postcondition" in {
    val unguarded = create(postconditionProg(""))
    unguarded.applicability(simpleAs).canApply should be(true)
    unguarded.meetsPostcondition(simpleAs) should be(false)
    unguarded.modify(simpleAs, SimpleProjectOperationArguments.Empty) match {
      case sm: SuccessfulModification =>
    }
  }

  it should "use always fails postcondition in same file" in {
    val guarded = create(postconditionProg("postcondition AlwaysGripe"))
    guarded.applicability(simpleAs).canApply should be(true)
    guarded.meetsPostcondition(simpleAs) should be(false)
    guarded.modify(simpleAs, SimpleProjectOperationArguments.Empty) match {
      case fm: FailedModificationAttempt =>
    }
  }

  it should "use verifying postcondition in same file" in {
    val guarded2 = create(postconditionProg("postcondition DidIt"))
    guarded2.applicability(simpleAs).canApply should be(true)
    guarded2.meetsPostcondition(simpleAs) should be(false)
    guarded2.modify(simpleAs, SimpleProjectOperationArguments.Empty) match {
      case sm: SuccessfulModification =>
        guarded2.meetsPostcondition(sm.result) should be(true)
    }
  }

  it should "use already met postcondition in same file" in {
    val guarded2 = create(postconditionProg("postcondition NeverGripes"))
    //guarded2.applicability(simpleAs).canApply should be (false)
    guarded2.meetsPostcondition(simpleAs) should be(true)
    guarded2.modify(simpleAs, SimpleProjectOperationArguments.Empty) match {
      case sm: NoModificationNeeded =>
    }
  }

  private def create(prog: String): ProjectEditor = {
    val filename = "whatever.txt"
    val runtime = new DefaultRugPipeline(DefaultTypeRegistry)
    val eds = runtime.createFromString(prog)
    val pe = eds.head.asInstanceOf[ProjectEditor]
    pe
  }
}