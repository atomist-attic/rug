package com.atomist.rug

import com.atomist.project.SimpleProjectOperationArguments
import com.atomist.project.edit._
import com.atomist.rug.InterpreterRugPipeline.DefaultRugArchive
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
        |with Project p
        |do
        |minorProblem "I'm a PITA"
      """.stripMargin)

  it should "use valid precondition in same file using predicate" in
    validPreconditionInSameFile(
      """
        |# Note that this doesn't ever gripe, but we are using the same name
        |predicate AlwaysGripe
        |with Project when false
      """.stripMargin)

  private  def validPreconditionInSameFile(alwaysGripePrecondition: String) {
    def prog(guard: Boolean) =
      s"""
         |editor Redeploy
         |
         |${if (guard) "precondition AlwaysGripe" else ""}
         |
         |with File f
         |do
         |  replace "some" "foo"
         |
         |Foo
         |
         |editor Foo
         |with File
         |do replace "content" "bar"
         |
         |$alwaysGripePrecondition
      """.stripMargin
    val unguarded = create(prog(false))
    assert(unguarded.applicability(simpleAs).canApply === true)
    unguarded.modify(simpleAs, SimpleProjectOperationArguments.Empty)
    match {
      case sm: SuccessfulModification =>
      
      case _ => ???
    }
    val guarded = create(prog(true))
    assert(guarded.applicability(simpleAs).canApply === false)
    guarded.modify(simpleAs, SimpleProjectOperationArguments.Empty)
    match {
      case nmn: NoModificationNeeded =>
      
      case _ => ???
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
         |with File f
         |do
         |  replace "some" "foo"
         |
         |predicate AlwaysTrue
         |with Project
      """.stripMargin

    val truePred1 =
      """
        |predicate Maybe
        |with File when name = "filename"
      """.stripMargin

    val truePred2 =
      """
        |predicate Maybe
        |with Project
        | with File when name = "filename"
      """.stripMargin

    val falsePred1 =
      """
        |predicate Maybe
        |with File when name = "certainlyNotThis"
      """.stripMargin

    val falsePred2 =
      """
        |predicate Maybe
        |# This won't match as it's not a Spring boot project
        |with SpringBootProject
      """.stripMargin

    val falsePred3 =
      """
        |predicate Maybe
        |# This won't match as it's not a Spring boot project
        |with JavaProject
      """.stripMargin

    val shouldDoIts = Seq(truePred1, truePred2).map(pred => prog + "\n\n" + pred)
    val shouldNotDoIts = Seq(falsePred1, falsePred2, falsePred3).map(pred => prog + "\n\n" + pred)

    for (doit <- shouldDoIts) {
      val ja = create(doit)
      assert(ja.applicability(simpleAs).canApply === true)
      ja.modify(simpleAs, SimpleProjectOperationArguments.Empty)
      match {
        case sm: SuccessfulModification =>
        
        case _ => ???
      }
    }

    for (dont <- shouldNotDoIts) {
      val nein = create(dont)
      assert(nein.applicability(simpleAs).canApply === false)
      nein.modify(simpleAs, SimpleProjectOperationArguments.Empty)
      match {
        case nmn: NoModificationNeeded =>
        
        case _ => ???
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
        |with File f
        |do
        |  replace "some" "foo"
        |
        |Foo
        |
        |editor Foo
        |
        |with File f
        |do replace "content" "bar"
        |
        |
        |reviewer NeverGripe
        |with File f
        |when "a" = "b"
        |do
        |minorProblem "I'm a PITA"
      """.stripMargin
    val harmlessGuard = create(prog)
    assert(harmlessGuard.applicability(simpleAs).canApply === true)
    harmlessGuard.modify(simpleAs, SimpleProjectOperationArguments.Empty)
  }

  it should "reject missing precondition" in {
    val prog =
      """
        |editor Redeploy
        |
        |precondition NeverGripe
        |
        |with File f
        |do
        |  replace "some" "foo"
        |Foo
        |
        |editor Foo
        |
        |with File f
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
        |with File f
        |do
        |  replace "some" "foo"
        |Foo
        |
        |editor Foo
        |
        |with File f
        |do replace "content" "bar"
        |
        |
        |reviewer NeverGripe
        |
        |param whatever: ^.*$
        |
        |with File f
        |when "a" = "b"
        |do
        |minorProblem "I'm a PITA"
        |
      """.stripMargin
    an[InvalidRugUsesException] should be thrownBy create(prog)
  }

  private  def postconditionProg(postcondition: String) =
    s"""
       |editor Redeploy
       |
       |$postcondition
       |
       |with File f
       |do
       |  replace "some" "foo"
       |
       |Foo
       |
       |editor Foo
       |
       |with File f
       |do replace "content" "bar"
       |
       |reviewer AlwaysGripe
       |
       |with Project p
       |do
       |minorProblem "I'm a PITA"
       |
       |reviewer NeverGripes
       |with File f when "a" = "b"
       |do minorProblem "I never get invoked"
       |
       |reviewer DidIt
       |
       |with File f
       |when { !f.content().contains("bar") }
       |do majorProblem "didn't run"
      """.stripMargin

  it should "use no postcondition" in {
    val unguarded = create(postconditionProg(""))
    assert(unguarded.applicability(simpleAs).canApply === true)
    unguarded.meetsPostcondition(simpleAs) should be(false)
    unguarded.modify(simpleAs, SimpleProjectOperationArguments.Empty) match {
      case sm: SuccessfulModification =>
      
      case _ => ???
    }
  }

  it should "use always fails postcondition in same file" in {
    val guarded = create(postconditionProg("postcondition AlwaysGripe"))
    assert(guarded.applicability(simpleAs).canApply === true)
    guarded.meetsPostcondition(simpleAs) should be(false)
    guarded.modify(simpleAs, SimpleProjectOperationArguments.Empty) match {
      case fm: FailedModificationAttempt =>
      
      case _ => ???
    }
  }

  it should "use verifying postcondition in same file" in {
    val guarded2 = create(postconditionProg("postcondition DidIt"))
    assert(guarded2.applicability(simpleAs).canApply === true)
    guarded2.meetsPostcondition(simpleAs) should be(false)
    guarded2.modify(simpleAs, SimpleProjectOperationArguments.Empty) match {
      case sm: SuccessfulModification =>
        guarded2.meetsPostcondition(sm.result) should be(true)
      case _ => ???
    }
  }

  it should "use already met postcondition in same file" in {
    val guarded2 = create(postconditionProg("postcondition NeverGripes"))
    //guarded2.applicability(simpleAs).canApply should be (false)
    guarded2.meetsPostcondition(simpleAs) should be(true)
    guarded2.modify(simpleAs, SimpleProjectOperationArguments.Empty) match {
      case sm: NoModificationNeeded =>
      
      case _ => ???
    }
  }

  private  def create(prog: String): ProjectEditor = {
    val filename = "whatever.txt"

    val runtime = new DefaultRugPipeline(DefaultTypeRegistry)
    val as = new SimpleFileBasedArtifactSource(DefaultRugArchive, StringFileArtifact(runtime.defaultFilenameFor(prog), prog))
    val eds = runtime.create(as,  None)

    val pe = eds.head.asInstanceOf[ProjectEditor]
    pe
  }
}