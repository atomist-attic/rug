package com.atomist.project.common.template

import com.atomist.source.EmptyArtifactSource
import org.scalatest.{FlatSpec, Matchers}

class MustacheMergeToolMergeStringTest extends FlatSpec with Matchers {

  import MustacheSamples._

  val mmt = new MustacheMergeTool(EmptyArtifactSource(""))

  it should "merge variable" in {
    val r = mmt.mergeString(FirstContext, First)
    r should equal(FirstExpected)
  }
}
