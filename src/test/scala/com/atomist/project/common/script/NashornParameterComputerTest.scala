package com.atomist.project.common.script

import com.atomist.param.SimpleParameterValue
import com.atomist.project.SimpleProjectOperationArguments
import com.atomist.util.script.Script
import org.scalatest.{FlatSpec, Matchers}

import scala.collection.JavaConversions._

object NashornParameterComputerTest {

  val emptyMap =
    s"""
       |function computed_parameters(m) {
       |  return {};
       |}
       |""".stripMargin

  val nullReturn =
    s"""
       |function computed_parameters(m) {
       |  return null;
       |}
       |""".stripMargin

  private def validCreatesOneParameterWithoutUsingMap(name: String, value: String) =
    s"""
       |function computed_parameters(m) {
       |  return { "$name": "$value" };
       |}
       |""".stripMargin

  def validCreatesOneParameterUsingMap(name: String) =
    s"""
       |function computed_parameters(m) {
       |  var v = m["pval"];
       |  return { $name: v };
       |}
       |""".stripMargin

  implicit def stringToScript(s: String): Script = Script("", s)

}

class NashornParameterComputerTest extends FlatSpec with Matchers {

  import NashornParameterComputerTest._

  it should "accept empty map" in {
    val npc = new NashornParameterComputer(emptyMap)
    npc.validate()
    val result = npc.computedParameters(null, null)
    result should be(empty)
  }

  it should "treat null return as empty map" in {
    val npc = new NashornParameterComputer(nullReturn)
    npc.validate()
    val result = npc.computedParameters(null, null)
    result should be(empty)
  }

  it should "work with valid file creating one parameter without using map" in {
    val (name, value) = ("pname", "aValue")
    val npc = new NashornParameterComputer(validCreatesOneParameterWithoutUsingMap(name, value))
    npc.validate()
    val result = npc.computedParameters(null, null)
    result.size should equal(1)
    result(0).getName should equal(name)
    result(0).getValue should equal(value)
  }

  it should "work with valid file creating one parameter using map" in {
    val (name, value) = ("pval", "maValue")

    val hasProperties = SimpleProjectOperationArguments("template-name",
      Seq(
        SimpleParameterValue(name, value)
      )
    )
    val npc = new NashornParameterComputer(validCreatesOneParameterUsingMap(name))
    npc.validate()
    val result = npc.computedParameters(null, hasProperties)
    result.size should equal(1)
    result(0).getName should equal(name)
    result(0).getValue should equal(value)
  }

  it should "work with valid file creating two parameters using map with string names" in {
    val validCreatesTwoParametersUsingMap =
      s"""
         |function computed_parameters(m) {
         |  var lib = "Turnbull";
         |  var lab = "Shorten";
         |  return { "lib" : lib, "lab": lab };
         |}
         |""".stripMargin

    val (name, value) = ("pval", "maValue")
    val hasProperties = SimpleProjectOperationArguments("template-name",
      Seq(
        SimpleParameterValue(name, value)
      )
    )
    val npc = new NashornParameterComputer(validCreatesTwoParametersUsingMap)
    npc.validate()
    val result = npc.computedParameters(null, hasProperties)
    result.size() should equal(2)
    result.exists(pv => "lab".equals(pv.getName) && "Shorten".equals(pv.getValue))
    result.exists(pv => "lib".equals(pv.getName) && "Turnbull".equals(pv.getValue))
  }

  it should "work with valid file creating two parameters using map with string values" in {
    val validCreatesTwoParametersUsingMap =
      s"""
         |function computed_parameters(m) {
         |  var lib = "Turnbull";
         |  var lab = "Shorten";
         |  return { lib : "pm", lab: "mp" };
         |}
         |""".stripMargin

    val (name, value) = ("pval", "maValue")
    val hasProperties = SimpleProjectOperationArguments("template-name",
      Seq(
        SimpleParameterValue(name, value)
      )
    )
    val npc = new NashornParameterComputer(validCreatesTwoParametersUsingMap)
    npc.validate()
    val result = npc.computedParameters(null, hasProperties)
    result.size() should equal(2)
    result.exists(pv => "Turnbull".equals(pv.getName) && "pm".equals(pv.getValue))
    result.exists(pv => "Shorten".equals(pv.getName) && "mp".equals(pv.getValue))
  }
}
