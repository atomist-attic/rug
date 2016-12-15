package com.atomist.rug.runtime.js

import com.atomist.project.archive.{AtomistConfig, DefaultAtomistConfig}
import com.atomist.rug.runtime.js.interop.AtomistFacade
import com.atomist.source.{ArtifactSource, FileArtifact}

object JavaScriptHandlerFinder {

  import com.atomist.rug.runtime.js.JavaScriptOperationFinder._

  private val notHasDeclareVarAtomist: FileArtifact => Boolean = f =>
    // TODO should fix this
    !(f.name.endsWith(".ts") && "atomist".r.findAllMatchIn(f.content).nonEmpty)

  /**
    * Find and handlers operations in the given Rug archive
    *
    * @param rugAs          archive to look into
    * @param atomist       facade to Atomist
    * @return a sequence of instantiated operations backed by JavaScript compiled
    *         from TypeScript
    */
  def registerHandlers(rugAs: ArtifactSource,
                       atomist: AtomistFacade,
                       atomistConfig: AtomistConfig = DefaultAtomistConfig): Unit = {
    val jsc = new JavaScriptContext

    jsc.engine.put("atomist", atomist)

    try {
      val compiled = filterAndCompile(rugAs, atomistConfig, notHasDeclareVarAtomist, jsc)

      for (f <- compiled.allFiles) {
        jsc.eval(f)
      }
    }
    finally {
      jsc.shutdown()
    }
  }

}
