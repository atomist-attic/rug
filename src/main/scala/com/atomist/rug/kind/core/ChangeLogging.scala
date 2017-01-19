package com.atomist.rug.kind.core

import com.atomist.rug.spi.ExportFunction

case class ChangeLogEntry[S](
                    comment: String,
                    resultState: S
                    )

/**
  * Extended by classes modeling mutable objects that undergo deliberate change
  */
trait ChangeLogging[S] {

  private var _changeLogEntries: Seq[ChangeLogEntry[S]] = Seq()

  protected def currentBackingObject: S

  /**
    * Describe a change we've made to this resource.
    * Does not necessarily map 1:1 to versions as in changeCount
    * @param comment message describing what we've done
    */
  @ExportFunction(readOnly = false, description = "Describe a change we made to this object")
  def describeChange(comment: String): Unit = {
    val le = new ChangeLogEntry[S](comment, currentBackingObject)
    _changeLogEntries = _changeLogEntries :+ le
  }

  def changeLogEntries: Seq[ChangeLogEntry[S]] = _changeLogEntries

}
