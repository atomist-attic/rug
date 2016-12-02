package com.atomist.project.common.template

import com.atomist.source.file.ClassPathArtifactSource
import com.atomist.source.{EmptyArtifactSource, SimpleFileBasedArtifactSource, StringFileArtifact}
import org.scalatest.{FlatSpec, Matchers}

class MustacheMergeToolTest extends FlatSpec with Matchers {

  import MustacheSamples._

  it should "fail with empty backing ArtifactSource" in {
    val mmt = new MustacheMergeTool(EmptyArtifactSource(""))
    an[IllegalArgumentException] should be thrownBy mmt.mergeToFile(FirstContext, "any.mustache")
  }

  it should "succeed with first template" in {
    val templateName = "first.mustache"
    val mmt = new MustacheMergeTool(new SimpleFileBasedArtifactSource("foo", StringFileArtifact(templateName, First)))
    for (i <- 1 to 3) {
      val r = mmt.mergeToFile(FirstContext, templateName).content
      r should equal(FirstExpected)
    }
  }

  val templateName = "first.mustache"
  val static1 = StringFileArtifact("static1", "test")
  val doubleDynamic = StringFileArtifact("location_was_{{in_ca}}.txt_.mustache", First)
  val straightTemplate = StringFileArtifact(templateName, First)
  val templateAs = new SimpleFileBasedArtifactSource("",
    Seq(
      straightTemplate,
      static1,
      doubleDynamic
    ))
  val cpTemplateAs = ClassPathArtifactSource.toArtifactSource("mustache/test.mustache")

  // This actually tests MergeHelper, not just MergeTool functionality
  it should "process template files" in {
    val mmt = new MustacheMergeTool(templateAs)
    val files = mmt.processTemplateFiles(FirstContext, templateAs.allFiles)
    files.size should equal(3)
    val expectedPath = "location_was_true.txt"
    // First.mustache
    files.map(f => f.path).toSet should equal(Set(static1.path, expectedPath, "first.mustache"))
    files.find(_.path.equals(expectedPath)).get.content should equal(FirstExpected)
  }

  it should "process classpath template files" in {
    val mmt = new MustacheMergeTool(cpTemplateAs)
    val files = mmt.processTemplateFiles(FirstContext, cpTemplateAs.allFiles)
    files.size should equal(1)
    val expectedPath = "G'day Chris. You just scored 10000 dollars. But the ATO has hit you with tax so you'll only get 6000.0"
    files.map(f => f.path).toSet should equal(Set("test.mustache"))
    files.head.content should equal(expectedPath)
  }

  it should "process template ArtifactSource" in {
    val mmt = new MustacheMergeTool(templateAs)
    val files = mmt.processTemplateFiles(FirstContext, templateAs).allFiles
    files.size should equal(3)
    val expectedPath = "location_was_true.txt"
    // First.mustache
    files.map(f => f.path).toSet should equal(Set(static1.path, expectedPath, "first.mustache"))
    files.find(_.path.equals(expectedPath)).get.content should equal(FirstExpected)
  }

  val mt = new MustacheMergeTool(new EmptyArtifactSource(""))

  it should "strip .mustache extension" in {
    val name = "template_.mustache"
    mt.toInPlaceFilePath(name) should equal ("template")
  }

  it should "strip .scaml extension" in {
    val name = "template_.scaml"
    mt.toInPlaceFilePath(name) should equal ("template")
  }
}
