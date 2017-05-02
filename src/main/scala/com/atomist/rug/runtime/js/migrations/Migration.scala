package com.atomist.rug.runtime.js.migrations

import com.atomist.source.ArtifactSource

/**
  * Interface to be implemented by migrations, which can modify
  * the ArtifactSource of a Rug to a presently compatible version.
  */
trait Migration {

  def name: String = getClass.getSimpleName.replace("$", "")

  /**
    * Human-readable description of this migration and why it was needed
    */
  def description: String

  /**
    * Perform the given migration, emitting log message(s) that can be displayed to the
    * user to encourage them to fix this Rug
    */
  def migrate(from: ArtifactSource, log: String => Unit): ArtifactSource

  def andThen(that: Migration): Migration =
    new Migration {
      override def name: String = s"${Migration.this.name}.${that.name}"

      override def description = s"${Migration.this.description}; ${that.description}"

      override def migrate(from: ArtifactSource, logger: String => Unit): ArtifactSource =
        that.migrate(Migration.this.migrate(from, logger), logger)
    }

}
