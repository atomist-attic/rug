package com.atomist.rug.runtime.js.migrations

/**
  * Keep this migration current to bring rugs up to date.
  */
object BringUpToDate {

  val Migration: Migration = NodeNameToProperty andThen NodeTagsToProperty

}
