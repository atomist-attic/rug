package com.atomist.rug.runtime.js.migrations

import com.atomist.source.{ArtifactSource, FileArtifact, SimpleFileEditor}

/**
  * Convenient superclass for migrations that do a simple find and replace in JS source
  */
abstract class LiteralJavaScriptReplacementMigration(val description: String, toReplace: String, replaceWith: String) extends Migration {

  final override def migrate(from: ArtifactSource, log: (String) => Unit): ArtifactSource = {
    def processFile(f: FileArtifact): FileArtifact = {
      if (f.content.contains(toReplace)) {
        val newContent = f.content.replaceAllLiterally(toReplace, replaceWith)
        log(s"Ran migration [$name] on [${f.path}]: $description")
        f.withContent(newContent)
      }
      else
        f
    }

    from ✎ SimpleFileEditor(f => f.path.startsWith(".atomist") && f.name.endsWith(".js"), processFile)
  }
}
