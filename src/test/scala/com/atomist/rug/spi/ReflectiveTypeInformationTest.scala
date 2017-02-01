package com.atomist.rug.spi

import com.atomist.rug.kind.DefaultTypeRegistry
import org.scalatest.{FlatSpec, Matchers}

class ReflectiveTypeInformationTest extends FlatSpec with Matchers {

  it should "find well known operations on well-known types" in {
    DefaultTypeRegistry.findByName("File").get.typeInformation match {
      case st: StaticTypeInformation =>
        st.operations.find(op => op.name.equals("name")) shouldBe (defined)
      case _ => ???
    }
  }
}
