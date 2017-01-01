package com.atomist.tree.pathexpression.marshal

import com.atomist.tree.pathexpression._
import org.scalatest.{FlatSpec, Matchers}

class JsonSerializerTest extends FlatSpec with Matchers {

  it should "serialize single location step expression" in {
    val pe = PathExpression(Seq(
      LocationStep(Child, NamedNodeTest("foo"), Seq(TruePredicate)))
    )
    val json = JsonSerializer.toJson(pe)
    //println(json)
    json.contains("Child\"") should be (true)
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
    val json = JsonSerializer.toJson(pe)
    //println(json)
  }

}
