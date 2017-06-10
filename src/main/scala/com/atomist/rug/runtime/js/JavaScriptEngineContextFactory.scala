package com.atomist.rug.runtime.js

import com.atomist.rug.runtime.js.nashorn.NashornContext
import com.atomist.source.ArtifactSource

/**
  * Create JavaScriptEngineContext
  */
object JavaScriptEngineContextFactory {
  def create(as: ArtifactSource): JavaScriptEngineContext = {
    new NashornContext(as)
  }
}
