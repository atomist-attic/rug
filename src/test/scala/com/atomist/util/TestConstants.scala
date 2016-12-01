package com.atomist.util

import com.atomist.tree.project.{SimpleResourceSpecifier, ResourceSpecifier}

object TestConstants {

  val AtomistGitHubOrg = "atomisthq"

  val AtomistProjectTemplatesGitHubOrg = "atomist-project-templates"

  private val IntrospectorLibRepoName = "introspector-lib"

  /**
    * The Maven groupId.
    */
  val AtomistGroupId = "com.atomist"
}

/**
  * Creates new instances of useful objects.
  */
object TestInstanceFactory {

  def newResourceSpecifier(): ResourceSpecifier = SimpleResourceSpecifier("groupId", "artifactId", "1.0")
}
