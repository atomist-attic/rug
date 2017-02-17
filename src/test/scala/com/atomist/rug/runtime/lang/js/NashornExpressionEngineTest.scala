package com.atomist.rug.runtime.lang.js

import com.atomist.param.SimpleParameterValues
import com.atomist.rug.runtime.rugdsl.SimpleFunctionInvocationContext
import org.scalatest.{FlatSpec, Matchers}

/**
  * Tests for handling JavaScript blocks and simple statements
  */
class NashornExpressionEngineTest extends FlatSpec with Matchers {

  it should "return int" in {
    val expr = "42"
    val fic = new SimpleFunctionInvocationContext[Object]("target", null, "", null, null, Map(), SimpleParameterValues.Empty, Nil)
    val eng = NashornExpressionEngine.evaluator(fic, expr)
    eng.evaluate(fic) should equal(42)
  }

  it should "return string" in {
    val expr = "'john' + ' smith'"
    val fic = new SimpleFunctionInvocationContext[Object]("target", null, "", null, null, Map(), SimpleParameterValues.Empty, Nil)
    val eng = NashornExpressionEngine.evaluator(fic, expr)
    eng.evaluate(fic) should equal("john smith")
  }

  it should "return a simple string with curly brackets in it" in {
    val expr = "return \"{}\""
    val fic = new SimpleFunctionInvocationContext[Object]("target", null, "", null, null, Map(), SimpleParameterValues.Empty, Nil)
    val eng = NashornExpressionEngine.evaluator(fic, expr)
    eng.evaluate(fic) should equal("{}")
  }

  it should "return a simple string with curly brackets in it using single quotes" in {
    val expr = "return '{}'"
    val fic = new SimpleFunctionInvocationContext[Object]("target", null, "", null, null, Map(), SimpleParameterValues.Empty, Nil)
    val eng = NashornExpressionEngine.evaluator(fic, expr)
    eng.evaluate(fic) should equal("{}")
  }

  it should "return block statement" in {
    val expr =
      """
        |if (true) {
        | return "dog"
        |}
        |else {
        | return "cat"
        |}
      """.stripMargin
    val fic = new SimpleFunctionInvocationContext[Object]("target", null, "", null, null, Map(), SimpleParameterValues.Empty, Nil)
    val eng = NashornExpressionEngine.evaluator(fic, expr)
    eng.evaluate(fic) should equal("dog")
  }
}
