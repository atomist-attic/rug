package com.atomist.rug.runtime

import com.atomist.param.Tag
import com.typesafe.scalalogging.LazyLogging

/**
  * For things common to _all_ Rugs
  */
trait Rug
  extends LazyLogging{

  def name: String

  def description: String

  def tags: Seq[Tag]



  def findRug(simpleOrFq: String) : Option[Rug]

  def findParameterizedRug(simpleOrFq: String) : Option[ParameterizedRug]

  def allRugs: Seq[Rug]

  /**
    * Add other rugs in the same archive to the context
    * @param rugs
    */
  def addToArchiveContext(rugs: Seq[Rug]) : Unit

  /**
    * For rugs referenced in the manifest outside of this rugs archive
    * @return
    */
  def externalContext: Seq[AddressableRug]

  /**
    * The set of rugs inside the same archive/runtime context
    * @return
    */
  def archiveContext: Seq[Rug]
}

/**
  * Like a rug, but we know a bit more about it
  */
trait AddressableRug extends Rug {
  def artifact: String
  def group: String
  def version: String
}
