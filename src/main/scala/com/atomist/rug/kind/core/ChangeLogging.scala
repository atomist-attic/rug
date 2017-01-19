package com.atomist.rug.kind.core

import com.atomist.rug.spi.ExportFunction

object ChangeLogging {

  type ChangeLogEntry = String
}

import ChangeLogging._

/**
  * Extended by classes modeling mutable objects that undergo deliberate change
  */
trait ChangeLogging {

  private var _changeLogEntries: Seq[ChangeLogEntry] = Seq()

  /**
    * Describe a change we've made to this resource.
    * Does not necessarily map 1:1 to versions as in changeCount
    * @param le message describing what we've done
    */
  @ExportFunction(readOnly = false, description = "Describe a change we made to this object")
  def describeChange(le: ChangeLogEntry): Unit = {
    _changeLogEntries = _changeLogEntries :+ le
  }

  def changeLogEntries: Seq[ChangeLogEntry] = _changeLogEntries

}
