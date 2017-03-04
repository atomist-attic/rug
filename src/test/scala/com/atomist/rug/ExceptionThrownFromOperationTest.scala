package com.atomist.rug

import com.atomist.source.EmptyArtifactSource
import org.scalatest.{FlatSpec, Matchers}

class ExceptionThrownFromOperationTest extends FlatSpec with Matchers {

  it should "handle exception voluntarily thrown by editor" in {
    val ed = TestUtils.editorInSideFile(this, "AlwaysFails.ts")
    try {
      ed.modify(EmptyArtifactSource())
      fail("Should've thrown exception")
    }
    catch {
      case rex: RuntimeException =>
        assert(rex.getMessage.contains("I have buddies who died face down in the muck"))
    }
  }

}
