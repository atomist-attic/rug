package com.atomist.project

import com.atomist.tree.content.project.SimpleResourceSpecifier
import org.scalatest.{FlatSpec, Matchers}

class ProjectOperationInfoTest extends FlatSpec with Matchers {

  import com.atomist.util.Utils.toOptional

  it should "not create default gav appropriately without group" in {
    val poi = SimpleProjectOperationInfo("name", "desc", Some("group"), None, Nil, Nil)
    assert(poi.gav.isPresent === false)
  }

  it should "not create default gav appropriately without version" in {
    val poi = SimpleProjectOperationInfo("name", "desc", None, Some("version"), Nil, Nil)
    assert(poi.gav.isPresent === false)
  }

  it should "create correct gav with group and version" in {
    val (group, version) = ("group", "version")
    val poi = SimpleProjectOperationInfo("name", "desc", Some(group), Some(version), Nil, Nil)
    assert(poi.gav.isPresent === true)
    assert(poi.gav.get === SimpleResourceSpecifier(group, poi.name, version))
  }
}
