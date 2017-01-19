package com.atomist.rug.kind.core

import com.atomist.rug.spi.ExportFunction

/**
  * Extended by classes modeling mutable objects that undergo deliberate change
  */
trait ChangeLogging {

  private var _changeLogEntries: Seq[String] = Seq()

  /**
    * Describe a change we've made to this resource.
    * Does not necessarily map 1:1 to versions as in changeCount
    * @param message message describing what we've done
    */
  @ExportFunction(readOnly = false, description = "Describe a change we made to this object")
  def describeChange(message: String): Unit = {
    _changeLogEntries = _changeLogEntries :+ message
  }

  def changeLogEntries: Seq[String] = _changeLogEntries

}
