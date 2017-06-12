package com.atomist.rug.runtime.js

import com.atomist.source.EmptyArtifactSource

object JavaScriptEngineTestUtils {
  def createEngine: JavaScriptEngineContext =
    JavaScriptEngineContextFactory.create(EmptyArtifactSource())
}
