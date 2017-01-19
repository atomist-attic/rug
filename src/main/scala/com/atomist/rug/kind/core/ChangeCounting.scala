package com.atomist.rug.kind.core

/**
  * Extended by classes with
  * versioned backing objects
  */
trait ChangeCounting {

  /**
    *
    * @return the number of revisions of this object. 0 means it has not
    *         been changed
    */
  def changeCount: Int

  def dirty: Boolean = changeCount > 0

}
