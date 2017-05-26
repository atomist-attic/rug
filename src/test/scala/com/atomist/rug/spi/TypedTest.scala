package com.atomist.rug.spi

import org.scalatest.{FlatSpec, Matchers}

class TypedTest extends FlatSpec with Matchers {

  import com.atomist.rug.kind.core.{ProjectMutableView, ProjectType}
  import com.atomist.rug.spi.Typed._

  it should "trim suffixes" in {
    trimSuffix("RemoveMe", "FirstPartRemoveMe") should be("FirstPart")
    trimSuffix("RemoveMe", "FirstPartRemoveMeNot") should be("FirstPartRemoveMeNot")
  }

  it should "map the Scala class to the Rug type name" in {
    typeClassToTypeName(classOf[ProjectType]) should be("Project")
    typeClassToTypeName(classOf[ProjectMutableView]) should be("ProjectMutableView")
  }

  it should "map the Scala type to the Rug type name" in {
    typeToTypeName(classOf[ProjectType]) should be("ProjectType")
    typeToTypeName(classOf[ProjectMutableView]) should be("Project")
  }

  it should "lower case the first character of a non-searchable type" in {
    typeToTypeName(classOf[ProjectMutableView], searchable = false) should be("project")
  }
}
