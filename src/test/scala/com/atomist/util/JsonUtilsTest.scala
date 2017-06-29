package com.atomist.util

import com.atomist.tree.pathexpression._
import org.scalatest.{FlatSpec, Matchers}

class JsonUtilsTest extends FlatSpec with Matchers {

  it should "serialize single location step expression" in {
    val pe = PathExpression(Seq(
      LocationStep(Child, NamedNodeTest("foo"), Seq(TruePredicate)))
    )
    val json = JsonUtils.toJsonPrettyPrint(pe)
    // println(json)
    json.contains("Child\"") shouldBe true
  }

  it should "support the wrapped json too" in {
    val pe = PathExpression(Seq(
      LocationStep(Child, NamedNodeTest("foo"), Seq(TruePredicate)))
    )
    val json = JsonUtils.toWrappedJson(pe)
    assert(json.contains("PathExpression") === true)
  }

  it should "serialize more complex step expression" in {
    val pe = PathExpression(Seq(
      LocationStep(Child, NamedNodeTest("foo"), Seq(TruePredicate)),
      LocationStep(
        NavigationAxis("resolvedBy"),
        NamedNodeTest("Commit"),
        Seq(
          PropertyValuePredicate("age", "25"),
          NodeNamePredicate("Commit")
        )
      )))
    val json = JsonUtils.toJsonStr(pe)
    // println(json)
  }

  it should "serialize expression with nested predicate" in {
    val expr = "/src//File()[/JavaType()]"
    val pe = PathExpressionParser.parsePathExpression(expr)
    val json = JsonUtils.toJsonPrettyPrint(pe)
    // println(json)
    json.contains("NestedPath") shouldBe true
  }
}
