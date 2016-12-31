package com.atomist.tree.pathexpression.marshal

import com.atomist.tree.pathexpression._
import org.scalatest.{FlatSpec, Matchers}

class JsonSerializerTest extends FlatSpec with Matchers {

  it should "serialize single location step expression" in {
    val pe = new PathExpression(Seq(
      LocationStep(Child, NamedNodeTest("foo"), Seq(TruePredicate)))
    )
    val json = JsonSerializer.toJson(pe)
    println(json)
  }

}
