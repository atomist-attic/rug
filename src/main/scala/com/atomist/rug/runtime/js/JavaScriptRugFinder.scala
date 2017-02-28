package com.atomist.rug.runtime.js

import com.atomist.rug.runtime.{AddressableRug, Rug}

/**
  * Interface for finding rugs of different types in Nashorn
  */
trait JavaScriptRugFinder[R <: Rug] {
  def find(jsc: JavaScriptContext, otherRugs: Seq[AddressableRug]) : Seq[R]
}
