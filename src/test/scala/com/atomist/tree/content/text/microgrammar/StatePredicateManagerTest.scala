package com.atomist.tree.content.text.microgrammar

import com.atomist.tree.content.text.microgrammar.predicates.CurlyDepth
import org.scalatest.{FlatSpec, Matchers}

class StatePredicateManagerTest extends FlatSpec with Matchers {

  it should "track curlies not in strings" in {
    var spm = new InputState("x{")
    spm = spm.register(new CurlyDepth)
    spm = spm.consume('x')
    spm.valueOf("curlyDepth") should be (Some(0))
    spm = spm.consume('{')
    spm.valueOf("curlyDepth") should be (Some(1))
  }

  it should "track curlies allowing for strings" in {
    var spm = new InputState("\"x{\"{")
    spm = spm.register(new CurlyDepth)
    spm = spm.consume('"')
    spm = spm.consume('x')
    spm.valueOf("curlyDepth") should be (Some(0))
    spm = spm.consume('{')
    spm.valueOf("curlyDepth") should be (Some(0))
    spm = spm.consume('"')
    spm = spm.consume('{')
    spm.valueOf("curlyDepth") should be (Some(1))
  }

}
