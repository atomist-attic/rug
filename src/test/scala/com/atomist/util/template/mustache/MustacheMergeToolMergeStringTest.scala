package com.atomist.util.template.mustache

import com.atomist.source.EmptyArtifactSource
import org.scalatest.{FlatSpec, Matchers}

class MustacheMergeToolMergeStringTest extends FlatSpec with Matchers {

  import com.atomist.util.template.mustache.MustacheSamples._

  val mmt = new MustacheMergeTool(EmptyArtifactSource(""))

  it should "merge variable" in {
    //    {
    //      "name": "Chris",
    //      "value": 10000,
    //      "taxed_value": 10000 - (10000 * 0.4),
    //      "in_ca": true
    //    }

    val r = mmt.mergeString(FirstContext, First)
    r should equal(FirstExpected)
  }

  /*
  // TODO need to decide on how to handle this case. Do we want to catch the exception, or just let it propagate as at present?
  it should "merge string with ill-formed reference" in pendingUntilFixed {
    val r = vmt.mergeString(mc, "this is a invalid template with $x$")
    r should equal(templateStringPrefix + v)
  }

  it should "merge string with {}-ed variable reference" in {
    val r = vmt.mergeString(mc, templateStringPrefix + "${" + k + "}")
    r should equal(templateStringPrefix + v)
  }

  it should "handle include from the backing ArtifactSource" in {
    val imp = StringFileArtifact("back.vm", "Hello $name")
    val backed = new VelocityMergeTool(new SimpleFileBasedArtifactSource("", imp))
    val mc = new MergeContext(Map("name" -> "Donald"))
    val s =
      """
        |I say hello to $name
        |#include ("back.vm")""".stripMargin
    val expected =
      """
        |I say hello to Donald
        |Hello $name""".stripMargin
    val r = backed.mergeString(mc, s)
    r should equal(expected)
  }

  it should "handle parse from the backing ArtifactSource" in {
    val imp = StringFileArtifact("back.vm", "Hello $name")
    val backed = new VelocityMergeTool(new SimpleFileBasedArtifactSource("", imp))
    val mc = new MergeContext(Map("name" -> "Donald"))
    val expected =
      """
        |I say hello to Donald
        |Hello Donald""".stripMargin
    val s =
      """
        |I say hello to $name
        |#parse ("back.vm")""".stripMargin
    val r = backed.mergeString(mc, s)
    r should equal(expected)
  }

  */
}
