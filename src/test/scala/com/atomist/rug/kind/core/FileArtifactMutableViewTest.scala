package com.atomist.rug.kind.core

import org.scalatest.{FlatSpec, Matchers}
import com.atomist.source.{FileArtifact, StringFileArtifact}

class FileArtifactMutableViewTest extends FlatSpec with Matchers {

  it should "handle nameContains" in {
    val f = StringFileArtifact("name", "The quick brown jumped over the lazy dog")
    val fmv = new FileArtifactMutableView(f, null)
    fmv.nameContains(".*") should be (false)
    fmv.nameContains("am") should be (true)
    fmv.nameContains("....") should be (false)
    fmv.nameContains("name") should be (true)
  }

  it should "return linecount in single line file" in {
    val f = StringFileArtifact("name", "The quick brown jumped over the lazy dog")
    val fmv = new FileArtifactMutableView(f, null)
    fmv.lineCount should be (1)
  }

  it should "return linecount in longer file" in {
    val f = StringFileArtifact("name",
      """
        |This is
        |a longer file
        |that will go on
        |and
        |on
        |and
        |on
      """.stripMargin)
    val fmv = new FileArtifactMutableView(f, null)
    fmv.lineCount should be (9)
  }

  it should "handle contains" in {
    val f = StringFileArtifact("name", "The quick brown jumped over the lazy dog")
    val fmv = new FileArtifactMutableView(f, null)
    fmv.contains(".*") should be (false)
    fmv.contains("brown") should be (true)
    fmv.contains("....") should be (false)
    fmv.contains("az") should be (true)
  }

  it should "handle containsMatch" in {
    val f = StringFileArtifact("name", "The quick brown jumped over the lazy dog")
    val fmv = new FileArtifactMutableView(f, null)
    fmv.containsMatch(".*") should be (true)
    fmv.containsMatch("[1-9]") should be (false)
    fmv.containsMatch("....") should be (true)
  }

  it should "setPath" in {
    val f = StringFileArtifact("name", "The quick brown jumped over the lazy dog")
    val fmv = new FileArtifactMutableView(f, null)
    fmv.path should equal (f.path)
    fmv.dirty should be (false)
    val path2 = "foobar/name"
    fmv.setPath(path2)
    fmv.dirty should be (true)
    fmv.path should equal (path2)
    fmv.currentBackingObject.path should equal (path2)
    fmv.originalBackingObject should equal (f)
  }

  it should "setName in root" in {
    val f = StringFileArtifact("name", "The quick brown jumped over the lazy dog")
    val fmv = new FileArtifactMutableView(f, null)
    fmv.name should equal("name")
    fmv.dirty should be (false)
    val name2 = "foobar"
    fmv.setName(name2)
    fmv.dirty should be (true)
    fmv.path should equal (name2)
    fmv.currentBackingObject.path should equal (name2)
    fmv.originalBackingObject should equal (f)
  }

  it should "setName under path" in {
    val f = StringFileArtifact("foo/name", "The quick brown jumped over the lazy dog")
    val fmv = new FileArtifactMutableView(f, null)
    fmv.name should equal("name")
    fmv.dirty should be (false)
    val name2 = "foobar"
    fmv.setName(name2)
    fmv.dirty should be (true)
    fmv.originalBackingObject should equal (f)
    fmv.currentBackingObject.path should equal ("foo/" + name2)
  }

  it should "verify permissions and unique id after setPath" is pending

  it should "replaceAll" in pending

  it should "replace" in pending

  it should "setContent" in {
    val f = StringFileArtifact("name", "The quick brown jumped over the lazy dog")
    val fmv = new FileArtifactMutableView(f, null)
    fmv.content should equal (f.content)
    fmv.dirty should be (false)
    val content2 = "To be or not to be"
    fmv.setContent(content2)
    fmv.dirty should be (true)
    fmv.content should equal (content2)
    fmv.originalBackingObject should equal (f)
  }

  it should "handle prepend" in {
    val f = StringFileArtifact("name", "The quick brown jumped over the lazy dog")
    val fmv = new FileArtifactMutableView(f, null)
    fmv.content should equal (f.content)
    fmv.dirty should be (false)
    val prepended = "To be or not to be"
    fmv.prepend(prepended)
    fmv.dirty should be (true)
    fmv.content should equal (prepended + f.content)
    fmv.originalBackingObject should equal (f)
  }

  it should "handle append" in {
    val f = StringFileArtifact("name", "The quick brown jumped over the lazy dog")
    val fmv = new FileArtifactMutableView(f, null)
    fmv.content should equal (f.content)
    fmv.dirty should be (false)
    val appended = "To be or not to be"
    fmv.append(appended)
    fmv.dirty should be (true)
    fmv.content should equal (f.content + appended)
    fmv.originalBackingObject should equal (f)
  }

  it should "return name" in {
    val f = StringFileArtifact("name", "The quick brown jumped over the lazy dog")
    val fmv = new FileArtifactMutableView(f, null)
    fmv.filename should equal (f.name)
  }

  it should "not add a string if the text already contains it" in {
    val initialContent: String = "The quick brown jumped"
    val f = StringFileArtifact("name",  initialContent)
    val fmv = new FileArtifactMutableView(f, null)

    val newString: String = " over the lazy dog"
    fmv.mustContain(newString)
    fmv.content should equal(initialContent + newString)
  }

  it should "add a string if it isn't already present" in {
    val initialContent: String = "The quick brown jumped over the lazy dog"
    val f = StringFileArtifact("name",  initialContent)
    val fmv = new FileArtifactMutableView(f, null)

    fmv.mustContain("over the lazy dog")
    fmv.content should equal(initialContent)
  }

  it should "not add a required string twice" in {
    val initialContent: String = "The quick brown jumped over the lazy dog"
    val f = StringFileArtifact("name",  initialContent)
    val fmv = new FileArtifactMutableView(f, null)

    fmv.mustContain("over the lazy dog")
    fmv.mustContain("over the lazy dog")
    fmv.content should equal(initialContent)
  }

  it should "make file executable" in {
    val f = StringFileArtifact("name.sh", "The quick brown jumped over the lazy dog")
    val fmv = new FileArtifactMutableView(f, null)
    fmv.name should equal("name.sh")
    fmv.dirty should be (false)
    fmv.makeExecutable()
    fmv.dirty should be (true)
    fmv.currentBackingObject.mode should equal (FileArtifact.ExecutableMode)
  }
}
