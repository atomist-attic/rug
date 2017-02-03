package com.atomist.util.lang

import org.scalatest.{FlatSpec, Matchers}

class JavaHelpersTest extends FlatSpec with Matchers {

  import com.atomist.util.lang.JavaHelpers._

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

  it should "convert to Java variable name" in {
    val s = "this_is_a_thing"
    toJavaVariableName(s) should equal ("thisIsAThing")
  }

  it should "camelize name" in {
    val s = "this_is_a_thing"
    toCamelizedPropertyName(s) should equal ("thisIsAThing")
  }

  it should "camelize name with $" in {
    val s = "t$his_is_a_thing"
    toCamelizedPropertyName(s) should equal ("t$hisIsAThing")
  }

  it should "uncamelize" in {
    val s = "thisIsAThing"
    toLowerCaseDelimited(s) should equal ("this_is_a_thing")
  }

  it should "property-ize" in {
    val s = "rug"
    propertyNameToGetterName(s) should equal ("getRug")
  }

  it should "upperize" in {
    val tests = Seq("a", "aa", "dude", "duder", "el duderino", "his dudeness")
    for (t <- tests) {
      val r = upperize(t)
      r.charAt(0).isUpper should be (true)
      r.substring(1) should equal (t.substring(1))
    }

  }

}
