package com.atomist.rug.rugdoc

import com.atomist.project.SimpleProjectOperationArguments
import com.atomist.project.edit.SuccessfulModification
import com.atomist.source.{EmptyArtifactSource, SimpleFileBasedArtifactSource, StringFileArtifact}
import org.scalatest.{FlatSpec, Matchers}

class TypeDocTest extends FlatSpec with Matchers {

  it should "generate type doc" in {
    val td = new TypeDoc()
    val output = td.generate(SimpleProjectOperationArguments.Empty)
    output.allFiles.size should be(1)
    val d = output.allFiles.head
  }

  it should "edit adding type doc if not present" in {
    val td = new TypeDoc()
    val output = td.modify(EmptyArtifactSource(""), SimpleProjectOperationArguments.Empty)
    output match {
      case sm: SuccessfulModification =>
        sm.result.allFiles.size should be(1)
        val d = sm.result.allFiles.head
    }
  }

  it should "edit type doc if present" in {
    val name = TypeDoc.DefaultDocName
    val td = new TypeDoc()
    val input = new SimpleFileBasedArtifactSource("",
      StringFileArtifact(name, "woieurowieur"))
    val output = td.modify(input,
      SimpleProjectOperationArguments.Empty)

    input.findFile(TypeDoc.DefaultDocName).get.contentLength should be < (20)

    output match {
      case sm: SuccessfulModification =>
        sm.result.allFiles.size should be(1)
        val d = sm.result.allFiles.head
        d.contentLength should be > (20)
    }
  }
}
