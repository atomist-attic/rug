package com.atomist.rug.runtime.rugdsl

import com.atomist.project.ProjectOperation
import com.atomist.rug.parser.RunOtherOperation
import com.atomist.rug.{Import, RugEditor, UndefinedRugUsesException}
import org.scalatest.{FlatSpec, Matchers}

class RugDrivenProjectOperationTest extends FlatSpec with Matchers {

  it should "throw a useful exception when a referenced operation is not found" in {
    val imports: Seq[Import] = Seq()
    val actions = Seq(RunOtherOperation("NonExistentEditor", Seq()))
    val program = RugEditor("name", None, Seq(), "description", imports, Seq(), None, Seq(), Seq(), actions, None)

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

}
