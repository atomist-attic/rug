package com.atomist.rug.runtime.js

import com.atomist.rug.runtime.js.nashorn.NashornContext
import com.atomist.rug.runtime.js.v8.V8JavaScriptEngineContext
import com.atomist.source.ArtifactSource

/**
  * Create JavaScriptEngineContext
  */
object JavaScriptEngineContextFactory {
  def create(as: ArtifactSource): JavaScriptEngineContext = {
    System.getProperty("rug.javascript.engine") match {
      case "nashorn" => new NashornContext(as)
      case _ => new V8JavaScriptEngineContext(as)
    }
  }
}
