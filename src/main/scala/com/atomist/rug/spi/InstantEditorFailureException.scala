package com.atomist.rug.spi

/**
  * Thrown when an editor should fail instantly
  *
  * @param msg message explaining failure
  */
class InstantEditorFailureException(msg: String) extends RuntimeException(msg)
