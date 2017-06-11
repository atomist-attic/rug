package com.atomist.rug.runtime.js.interop

import com.atomist.rug.runtime.js.nashorn.NashornContext
import com.atomist.source.EmptyArtifactSource

object NashornUtilsTest {

  def createEngine: NashornContext = new NashornContext(EmptyArtifactSource())

}
