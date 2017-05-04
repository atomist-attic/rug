package com.atomist.rug.runtime.js.migrations

object NodeNameToProperty extends LiteralJavaScriptReplacementMigration("GraphNode.nodeName is now a property, not a function", "nodeName()", "nodeName")

object NodeTagsToProperty extends LiteralJavaScriptReplacementMigration("GraphNode.nodeTags is now a property, not a function", "nodeTags()", "nodeTags")