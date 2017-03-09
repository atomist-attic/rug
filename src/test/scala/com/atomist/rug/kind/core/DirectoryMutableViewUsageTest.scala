package com.atomist.rug.kind.core

import com.atomist.parse.java.ParsingTargets
import com.atomist.project.edit.NoModificationNeeded
import com.atomist.rug.TestUtils
import org.scalatest.{FlatSpec, Matchers}

class DirectoryMutableViewUsageTest extends FlatSpec with Matchers {

  "DirectoryMutableView" should "#406: be accessible from editor" in {
    val ed = TestUtils.editorInSideFile(this, "AccessDirectory.ts")
    ed.modify(ParsingTargets.NewStartSpringIoProject) match {
      case _: NoModificationNeeded =>
      case x => fail(s"Unexpected $x")
    }
  }

}
