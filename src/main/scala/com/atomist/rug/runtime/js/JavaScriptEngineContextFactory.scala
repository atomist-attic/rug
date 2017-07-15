package com.atomist.rug.runtime.js

import com.atomist.rug.runtime.js.nashorn.NashornJavaScriptEngine
import com.atomist.rug.runtime.js.v8.V8JavaScriptEngine
import com.atomist.source.ArtifactSource

/**
  * Create JavaScriptEngineContext
  */
object JavaScriptEngineContextFactory {
  def create(as: ArtifactSource): JavaScriptEngineContext = {
    System.getProperty("rug.javascript.engine") match {
      case "nashorn" => new NashornJavaScriptEngine(as)
      case _ => new V8JavaScriptEngine(as)
    }
  }
}
