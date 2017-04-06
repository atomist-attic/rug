package com.atomist.rug.runtime.js

import com.atomist.project.archive.RugResolver
import com.atomist.rug.runtime.Rug

/**
  * Interface for finding rugs of different types in Nashorn
  */
trait JavaScriptRugFinder[R <: Rug] {
  def find(jsc: JavaScriptContext, resolver: Option[RugResolver]) : Seq[R]
}
