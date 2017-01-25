package com.atomist.rug.runtime.js.interop

import com.atomist.parse.java.ParsingTargets
import com.atomist.project.SimpleProjectOperationArguments
import com.atomist.project.edit.{NoModificationNeeded, SuccessfulModification}
import com.atomist.rug.TestUtils
import org.scalatest.{FlatSpec, Matchers}

class JavaScriptBackedTypeProviderTest extends FlatSpec with Matchers {

  it should "invoke tree finder with one level only" in {
    val jsed = TestUtils.editorInSideFile(this, "SimpleBanana.ts")
    val target = ParsingTargets.SpringIoGuidesRestServiceSource
    jsed.modify(target, SimpleProjectOperationArguments.Empty) match {
      case nmn: NoModificationNeeded =>
    }
  }

  it should "invoke tree finder with two levels" in {
    val jsed = TestUtils.editorInSideFile(this, "TwoLevel.ts")
    val target = ParsingTargets.SpringIoGuidesRestServiceSource
    jsed.modify(target, SimpleProjectOperationArguments.Empty) match {
      case nmn: NoModificationNeeded =>
    }
  }

  it should "invoke side effecting tree finder with two levels" in {
    val jsed = TestUtils.editorInSideFile(this, "MutatingBanana.ts")
    val target = ParsingTargets.SpringIoGuidesRestServiceSource
    jsed.modify(target, SimpleProjectOperationArguments.Empty) match {
      case sm: SuccessfulModification =>
        sm.result.allFiles.exists(f => f.name.endsWith(".java") && f.content.startsWith("I am evil!"))
    }
  }

}
