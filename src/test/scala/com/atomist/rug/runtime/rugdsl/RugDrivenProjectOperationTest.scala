package com.atomist.rug.runtime.rugdsl

import com.atomist.param.{Parameter, Tag}
import com.atomist.project.ProjectOperation
import com.atomist.rug.parser.RunOtherOperation
import com.atomist.rug.{Import, RugEditor, UndefinedRugUsesException}
import org.scalatest.{FlatSpec, Matchers}

class RugDrivenProjectOperationTest extends FlatSpec with Matchers {

  it should "throw a useful exception when a referenced operation is not found" in {
    val imports: Seq[Import] = Seq()
    val actions = Seq(RunOtherOperation("NonExistentEditor", Seq()))
    val program = RugEditor("name", None, Seq(), "description", imports, Seq(), None, Seq(), Seq(), actions)

    val knownOperations: Seq[ProjectOperation] = Seq()

    try {
      RugDrivenProjectOperation.validateUses(program, None, "UnhappyProgram", knownOperations)
      fail("That was supposed to complain")
    }
    catch {
      case e: UndefinedRugUsesException =>
        e.getMessage.contains("not found when processing operation") should be(true)
    }
  }

  it should "throw a really useful exception when you forgot to declare a referenced operation" in {
    val imports: Seq[Import] = Seq()
    val actions = Seq(RunOtherOperation("OperationOfYay", Seq()))
    val program = RugEditor("name", None, Seq(), "description", imports, Seq(), None, Seq(), Seq(), actions)

    val knownOperations: Seq[ProjectOperation] = Seq(projectOperationCalled("some.namespaced.OperationOfYay"))

    try {
      RugDrivenProjectOperation.validateUses(program, None, "UnhappyProgram", knownOperations)
      fail("That was supposed to complain")
    }
    catch {
      case e: UndefinedRugUsesException =>
        withClue ("exception was: " + e.getMessage) {
          e.getMessage.contains("uses some.namespaced.Operation") should be(true)
        }
    }
  }

  def projectOperationCalled(_name: String) =  new ProjectOperation {
    override def name: String = _name

    override def description: String = ???

    override def tags: Seq[Tag] = ???

    override def parameters: Seq[Parameter] = ???
  }

}
