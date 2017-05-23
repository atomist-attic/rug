package com.atomist.rug.spi

import com.atomist.rug.kind.DefaultTypeRegistry
import org.scalatest.{FlatSpec, Matchers}

class ReflectiveTypeOperationFinderTest extends FlatSpec with Matchers {

  it should "find well known operations on well-known types" in {
    val st = DefaultTypeRegistry.findByName("YamlFile").get
    st.allOperations.find(_.name == "children") shouldBe defined
  }

  it should "not find property on parent" in pendingUntilFixed {
    val st = DefaultTypeRegistry.findByName("YamlFile").get

    println(s"There are ${st.operations.length} operations")
    println(s"There are ${st.allOperations.length} allOperations")

    st.operations.find(_.name == "children") shouldBe empty
  }
}
