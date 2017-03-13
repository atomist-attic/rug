package com.atomist.rug.spi

import com.atomist.rug.kind.DefaultTypeRegistry
import org.scalatest.{FlatSpec, Matchers}

class ReflectiveTypeOperationFinderTest extends FlatSpec with Matchers {

  it should "find well known operations on well-known types" in {
    val st = DefaultTypeRegistry.findByName("YamlFile").get
    st.allOperations.find(_.name == "value") shouldBe defined
  }

  it should "not find property on parent" in {
    val st = DefaultTypeRegistry.findByName("YamlFile").get
    st.operations.find(_.name == "value") shouldBe empty
  }
}
