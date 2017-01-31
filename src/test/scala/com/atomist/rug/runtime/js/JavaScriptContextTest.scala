package com.atomist.rug.runtime.js

import com.atomist.rug.RugJavaScriptException
import com.atomist.source.{ SimpleFileBasedArtifactSource, StringFileArtifact}
import org.scalatest.{FlatSpec, Matchers}

class JavaScriptContextTest extends FlatSpec with Matchers {
  val SimpleEditorWithoutParameters: String =
    """
      |guff js?
    """.stripMargin

  it should "throw an exception containing the JS file name if there are error during eval" in {
    val filename = ".atomist/editors/SimpleEditor.js"
    val caught = intercept[RugJavaScriptException] {
      new JavaScriptContext(SimpleFileBasedArtifactSource(StringFileArtifact(filename, SimpleEditorWithoutParameters)))
    }
    caught.getMessage should include(filename)
  }
}
