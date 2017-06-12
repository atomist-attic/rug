package com.atomist.rug.runtime.js.interop

import com.atomist.param.SimpleParameterValues
import com.atomist.parse.java.ParsingTargets
import com.atomist.rug.TestUtils
import org.scalatest.{FlatSpec, Matchers}

class RuntimeExceptionHandlingTest extends FlatSpec with Matchers {

  it should "report correct position for null pointer exception in single file" in {
    val jsed = TestUtils.editorInSideFile(this, "DeliberateNpe.ts")
    val target = ParsingTargets.SpringIoGuidesRestServiceSource
    try {
      jsed.modify(target, SimpleParameterValues.Empty)
    }
    catch {
      case e: SourceLanguageRuntimeException =>
        e.jsRuntimeErrorInfo should not be null
        e.pos should not be null
        assert(e.sourceLangRuntimeErrorInfo.filePath.endsWith("/DeliberateNpe.ts"))
        assert(e.jsRuntimeErrorInfo.filePath.endsWith("/DeliberateNpe.js"))
        assert(e.sourceLangRuntimeErrorInfo.pos.lineFrom1 == 8)
    }
  }

  it should "report correct position for null pointer exception in multiple files" in {
    val rugs = TestUtils.rugsInSideFile(this, "DeliberateNpe.ts", "HandlerWithFailingRugFunction.ts")
    val jsed = rugs.editors.find(_.name.indexOf("FunctionKiller") == -1).get
    val target = ParsingTargets.SpringIoGuidesRestServiceSource
    try {
      jsed.modify(target, SimpleParameterValues.Empty)
    }
    catch {
      case e: SourceLanguageRuntimeException =>
        e.jsRuntimeErrorInfo should not be null
        e.pos should not be null
        assert(e.sourceLangRuntimeErrorInfo.filePath.endsWith("/DeliberateNpe.ts"))
        assert(e.jsRuntimeErrorInfo.filePath.endsWith("/DeliberateNpe.js"))
        assert(e.sourceLangRuntimeErrorInfo.pos.lineFrom1 == 8)
    }
  }

}
