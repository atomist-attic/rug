package com.atomist.project

import com.atomist.tree.project.SimpleResourceSpecifier
import org.scalatest.{FlatSpec, Matchers}

class ProjectOperationInfoTest extends FlatSpec with Matchers {

  import com.atomist.util.Utils.toOptional

  import scala.collection.JavaConversions._

  it should "not create default gav appropriately without group" in {
    val poi = SimpleProjectOperationInfo("name", "desc", Some("group"), None, Nil, Nil)
    poi.gav.isPresent should be(false)
  }

  it should "not create default gav appropriately without version" in {
    val poi = SimpleProjectOperationInfo("name", "desc", None, Some("version"), Nil, Nil)
    poi.gav.isPresent should be(false)
  }

  it should "create correct gav with group and version" in {
    val (group, version) = ("group", "version")
    val poi = SimpleProjectOperationInfo("name", "desc", Some(group), Some(version), Nil, Nil)
    poi.gav.isPresent should be(true)
    poi.gav.get should equal(SimpleResourceSpecifier(group, poi.name, version))
  }
}
