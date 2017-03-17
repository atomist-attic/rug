package com.atomist.rug.runtime.js.interop

import jdk.nashorn.api.scripting.{NashornScriptEngine, NashornScriptEngineFactory}

object NashornUtilsTest {

  def createEngine: NashornScriptEngine =
    new NashornScriptEngineFactory()
      .getScriptEngine("--optimistic-types", "--language=es6", "--no-java")
      .asInstanceOf[NashornScriptEngine]

}
