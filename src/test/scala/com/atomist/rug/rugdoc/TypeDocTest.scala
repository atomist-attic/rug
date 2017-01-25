package com.atomist.rug.rugdoc

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}

import com.atomist.param.SimpleParameterValues
import com.atomist.project.edit.SuccessfulModification
import com.atomist.source.{EmptyArtifactSource, SimpleFileBasedArtifactSource, StringFileArtifact}
import org.scalatest.{FlatSpec, Matchers}

class TypeDocTest extends FlatSpec with Matchers {

  it should "generate type doc" in {
    val td = new TypeDoc()
    val output = td.generate("", SimpleParameterValues.Empty)
    assert(output.allFiles.size === 1)
    val d = output.allFiles.head
    d.contentLength should be > (100000)
    val doc: String = d.content
    doc.contains("Type: `${kind.name}`") should be (false)
    doc.contains("${op.description}") should be (false)
    doc.contains("`${op.name}` Parameters") should be (false)
    doc.contains("***${p.name}***") should be (false)
    // the checks below are hard-coded and therefore somewhat fragile
    doc.contains("\n## Type: `Project`\n") should be (true)
    doc.contains("\n#### Operation: `eval`\n") should be (true)
    doc.contains("*The regular expression to search for*") should be (true)
    Files.write(Paths.get("target/RugTypes.md"), doc.getBytes(StandardCharsets.UTF_8))
  }

  it should "edit adding type doc if not present" in {
    val td = new TypeDoc()
    val output = td.modify(EmptyArtifactSource(""), SimpleParameterValues.Empty)
    output match {
      case sm: SuccessfulModification =>
        assert(sm.result.allFiles.size === 1)
        val d = sm.result.allFiles.head
      case _ => ???
    }
  }

  it should "edit type doc if present" in {
    val name = TypeDoc.DefaultDocName
    val td = new TypeDoc()
    val input = new SimpleFileBasedArtifactSource("",
      StringFileArtifact(name, "woieurowieur"))
    val output = td.modify(input,
      SimpleParameterValues.Empty)

    input.findFile(TypeDoc.DefaultDocName).get.contentLength should be < (20)

    output match {
      case sm: SuccessfulModification =>
        assert(sm.result.allFiles.size === 1)
        val d = sm.result.allFiles.head
        d.contentLength should be > (20)
      case _ => ???
    }
  }
}
