package com.atomist.project.common.template

import com.atomist.project.common.template.{MergeContext, VelocityMergeTool}
import com.atomist.source.{EmptyArtifactSource, SimpleFileBasedArtifactSource, StringFileArtifact}
import org.scalatest.{FlatSpec, Matchers}

class VelocityMergeToolMergeStringTest extends FlatSpec with Matchers {

  val vmt = new VelocityMergeTool(EmptyArtifactSource(""))
  val k = "thing"
  val v = "otherThing"
  val mc = MergeContext(Map(k -> v))
  val templateStringPrefix = s"We have the power to transform the "

  it should "merge string with simple variable reference" in {
    val r = vmt.mergeString(mc, templateStringPrefix + "$" + k)
    r should equal(templateStringPrefix + v)
  }

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
    val mc = MergeContext(Map("name" -> "Donald"))
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
    val mc = MergeContext(Map("name" -> "Donald"))
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
}
