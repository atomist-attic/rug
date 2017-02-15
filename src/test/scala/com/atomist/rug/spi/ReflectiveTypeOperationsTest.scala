package com.atomist.rug.spi

import com.atomist.rug.kind.DefaultTypeRegistry
import org.scalatest.{FlatSpec, Matchers}

class ReflectiveTypeOperationsTest extends FlatSpec with Matchers {

  it should "find well known operations on well-known types" in {
    val st = DefaultTypeRegistry.findByName("File").get.typeInformation
    st.operations.find(op => op.name.equals("name")) shouldBe (defined)
  }
}
