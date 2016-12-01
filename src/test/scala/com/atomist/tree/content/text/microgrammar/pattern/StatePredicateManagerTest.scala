package com.atomist.tree.content.text.microgrammar.pattern

import com.atomist.tree.content.text.microgrammar.{CurlyDepth, StatePredicateManager}
import org.scalatest.{FlatSpec, Matchers}

class StatePredicateManagerTest extends FlatSpec with Matchers {

  it should "track curlies not in strings" in {
    val spm = new StatePredicateManager
    spm.register(new CurlyDepth)
    spm.consume('x')
    spm.valueOf("curlyDepth") should be (Some(0))
    spm.consume('{')
    spm.valueOf("curlyDepth") should be (Some(1))
  }

  it should "track curlies allowing for strings" in {
    val spm = new StatePredicateManager
    spm.register(new CurlyDepth)
    spm.consume('"')
    spm.consume('x')
    spm.valueOf("curlyDepth") should be (Some(0))
    spm.consume('{')
    spm.valueOf("curlyDepth") should be (Some(0))
    spm.consume('"')
    spm.consume('{')
    spm.valueOf("curlyDepth") should be (Some(1))
  }

}
