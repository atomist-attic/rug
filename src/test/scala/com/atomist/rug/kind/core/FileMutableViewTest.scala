package com.atomist.rug.kind.core

import org.scalatest.{FlatSpec, Matchers}
import com.atomist.source.{FileArtifact, StringFileArtifact}

class FileMutableViewTest extends FlatSpec with Matchers {

  "FileMutableView" should "handle nameContains" in {
    val f = StringFileArtifact("name", "The quick brown jumped over the lazy dog")
    val fmv = new FileMutableView(f, null)
    fmv.nameContains(".*") should be (false)
    fmv.nameContains("am") should be (true)
    fmv.nameContains("....") should be (false)
    fmv.nameContains("name") should be (true)
  }

  it should "return contentLength" in {
    val s =
      """
        |I'm talkin' about ethics
        |String men also cry
        |This aggression will not stand
      """.stripMargin
    val f = StringFileArtifact("name", s)
    val fmv = new FileMutableView(f, null)
    assert(fmv.contentLength === s.length)
  }

  it should "return linecount in single line file" in {
    val f = StringFileArtifact("name", "The quick brown jumped over the lazy dog")
    val fmv = new FileMutableView(f, null)
    assert(fmv.lineCount === 1)
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
    val fmv = new FileMutableView(f, null)
    assert(fmv.lineCount === 9)
  }

  it should "handle contains" in {
    val f = StringFileArtifact("name", "The quick brown jumped over the lazy dog")
    val fmv = new FileMutableView(f, null)
    fmv.contains(".*") should be (false)
    fmv.contains("brown") should be (true)
    fmv.contains("....") should be (false)
    fmv.contains("az") should be (true)
  }

  it should "handle containsMatch" in {
    val f = StringFileArtifact("name", "The quick brown jumped over the lazy dog")
    val fmv = new FileMutableView(f, null)
    fmv.containsMatch(".*") should be (true)
    fmv.containsMatch("[1-9]") should be (false)
    fmv.containsMatch("....") should be (true)
  }

  it should "setPath" in {
    val f = StringFileArtifact("name", "The quick brown jumped over the lazy dog")
    val fmv = new FileMutableView(f, null)
    assert(fmv.path === f.path)
    assert(fmv.dirty === false)
    val path2 = "foobar/name"
    fmv.setPath(path2)
    assert(fmv.dirty === true)
    assert(fmv.path === path2)
    assert(fmv.currentBackingObject.path === path2)
    assert(fmv.originalBackingObject === f)
  }

  it should "setName in root" in {
    val f = StringFileArtifact("name", "The quick brown jumped over the lazy dog")
    val fmv = new FileMutableView(f, null)
    assert(fmv.name === "name")
    assert(fmv.dirty === false)
    val name2 = "foobar"
    fmv.setName(name2)
    assert(fmv.dirty === true)
    assert(fmv.path === name2)
    assert(fmv.currentBackingObject.path === name2)
    assert(fmv.originalBackingObject === f)
  }

  it should "setName under path" in {
    val f = StringFileArtifact("foo/name", "The quick brown jumped over the lazy dog")
    val fmv = new FileMutableView(f, null)
    assert(fmv.name === "name")
    assert(fmv.dirty === false)
    val name2 = "foobar"
    fmv.setName(name2)
    assert(fmv.dirty === true)
    assert(fmv.originalBackingObject === f)
    assert(fmv.currentBackingObject.path === "foo/" + name2)
  }

  it should "verify permissions and unique id after setPath" is pending

  it should "replaceAll" in pending

  it should "replace" in pending

  it should "setContent" in {
    val f = StringFileArtifact("name", "The quick brown jumped over the lazy dog")
    val fmv = new FileMutableView(f, null)
    assert(fmv.content === f.content)
    assert(fmv.dirty === false)
    val content2 = "To be or not to be"
    fmv.setContent(content2)
    assert(fmv.dirty === true)
    assert(fmv.content === content2)
    assert(fmv.originalBackingObject === f)
  }

  it should "handle prepend" in {
    val f = StringFileArtifact("name", "The quick brown jumped over the lazy dog")
    val fmv = new FileMutableView(f, null)
    assert(fmv.content === f.content)
    assert(fmv.dirty === false)
    val prepended = "To be or not to be"
    fmv.prepend(prepended)
    assert(fmv.dirty === true)
    assert(fmv.content === prepended + f.content)
    assert(fmv.originalBackingObject === f)
  }

  it should "handle append" in {
    val f = StringFileArtifact("name", "The quick brown jumped over the lazy dog")
    val fmv = new FileMutableView(f, null)
    assert(fmv.content === f.content)
    assert(fmv.dirty === false)
    val appended = "To be or not to be"
    fmv.append(appended)
    assert(fmv.dirty === true)
    assert(fmv.content === f.content + appended)
    assert(fmv.originalBackingObject === f)
  }

  it should "return name" in {
    val f = StringFileArtifact("name", "The quick brown jumped over the lazy dog")
    val fmv = new FileMutableView(f, null)
    assert(fmv.filename === f.name)
  }

  it should "not add a string if the text already contains it" in {
    val initialContent: String = "The quick brown jumped"
    val f = StringFileArtifact("name",  initialContent)
    val fmv = new FileMutableView(f, null)

    val newString: String = " over the lazy dog"
    fmv.mustContain(newString)
    assert(fmv.content === initialContent + newString)
  }

  it should "add a string if it isn't already present" in {
    val initialContent: String = "The quick brown jumped over the lazy dog"
    val f = StringFileArtifact("name",  initialContent)
    val fmv = new FileMutableView(f, null)

    fmv.mustContain("over the lazy dog")
    assert(fmv.content === initialContent)
  }

  it should "not add a required string twice" in {
    val initialContent: String = "The quick brown jumped over the lazy dog"
    val f = StringFileArtifact("name",  initialContent)
    val fmv = new FileMutableView(f, null)

    fmv.mustContain("over the lazy dog")
    fmv.mustContain("over the lazy dog")
    assert(fmv.content === initialContent)
  }

  it should "make file executable" in {
    val f = StringFileArtifact("name.sh", "The quick brown jumped over the lazy dog")
    val fmv = new FileMutableView(f, null)
    assert(fmv.name === "name.sh")
    assert(fmv.dirty === false)
    fmv.makeExecutable()
    assert(fmv.dirty === true)
    assert(fmv.currentBackingObject.mode === FileArtifact.ExecutableMode)
  }
}
