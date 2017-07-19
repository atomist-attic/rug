package com.atomist.rug.runtime.js

import com.atomist.source.EmptyArtifactSource

object JavaScriptEngineTestUtils {
  def createEngine: JavaScriptEngine =
    JavaScriptEngineContextFactory.create(EmptyArtifactSource())
}
