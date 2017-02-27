package com.atomist.rug.kind.xml

import com.atomist.rug.kind.DefaultTypeRegistry
import org.scalatest.{FlatSpec, Matchers}

/**
  * The XmlMutableViewTest tests it...we just need to
  * check that it's registered
  */
class XmlTypeRegistrationTest extends FlatSpec with Matchers {

  it should "be registered" in {
    DefaultTypeRegistry.findByName("Xml") should not be null
  }
}
