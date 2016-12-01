package com.atomist.project

import com.atomist.tree.project.{ResourceSpecifier, SimpleResourceSpecifier}
import com.atomist.param.{Parameterized, Tag}

trait ProjectOperationInfo extends Parameterized {

  def name: String

  def description: String

  def group: Option[String] = None

  def version: Option[String] = None

  def gav: Option[ResourceSpecifier] = {
    val rs = if (group.isDefined && version.isDefined)
      SimpleResourceSpecifier(group.get, name, version.get)
    else
      null

    Option(rs)
  }

  def tags: Seq[Tag]
}
