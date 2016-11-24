package com.atomist.rug.kind.java.support

import org.scalatest.{Matchers, FlatSpec}

class JavaHelpersTest extends FlatSpec with Matchers {

  import JavaHelpers._

  it should "find package of FQN in non-default package" in {
    val fqn1 = "com.something.Foo"
    packageFor(fqn1) should equal ("com.something")
  }

  it should "find package of FQN in default package" in {
    val fqn1 = "Foo"
    packageFor(fqn1) should equal ("")
  }

  it should "recognize valid packages" in {
    val validPackages = Seq("", "com", "com.foo", "com.$foo.bar_", "com.xy.z.a2")
    for (p <- validPackages)
      isValidPackageName(p) should be (true)
  }

  it should "recognize invalid packages" in {
    val validPackages = Seq("1", "1com", "com.1foo", "com.$foo.bar.", "com.xy.z.a&2.", "#", "1234a")
    for (p <- validPackages)
      isValidPackageName(p) should be (false)
  }

}
