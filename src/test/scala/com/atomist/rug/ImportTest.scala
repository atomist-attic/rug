package com.atomist.rug

import org.scalatest.{FlatSpec, Matchers}

class ImportTest extends FlatSpec with Matchers {

  val simpleName1 = "Foobar"

  it should "get simple name from default package" in {
    val imp = Import(simpleName1)
    imp.simpleName should equal (simpleName1)
  }

  it should "get simple name from non-default package" in {
    val imp = Import(s"com.foo.bar.$simpleName1")
    imp.simpleName should equal (simpleName1)
  }
}
