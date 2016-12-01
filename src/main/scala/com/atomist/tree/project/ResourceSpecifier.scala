package com.atomist.tree.project

import java.util.Objects

import com.fasterxml.jackson.annotation.JsonProperty

import scala.annotation.meta.field

/**
  * Represents a published artifact: for example, in a Maven repository.
  */
trait ResourceSpecifier {

  def groupId: String

  def artifactId: String

  def version: String

  override def toString = s"${groupId}_${artifactId}_$version"

  override def equals(o: Any) = o match {
    case that: ResourceSpecifier =>
      Objects.equals(groupId, that.groupId) &&
        Objects.equals(artifactId, that.artifactId) &&
        Objects.equals(version, that.version)
    case _ => false
  }
}

case class SimpleResourceSpecifier(
                                    @(JsonProperty@field)("group_id") groupId: String,
                                    @(JsonProperty@field)("artifact_id") artifactId: String,
                                    @(JsonProperty@field)("version") version: String)
  extends ResourceSpecifier

object UnidentifiedResourceSpecifier extends SimpleResourceSpecifier("", "", "")
