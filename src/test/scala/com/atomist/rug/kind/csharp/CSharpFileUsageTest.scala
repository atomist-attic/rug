package com.atomist.rug.kind.csharp

import com.atomist.project.SimpleProjectOperationArguments
import com.atomist.project.edit.{ModificationAttempt, NoModificationNeeded, SuccessfulModification}
import com.atomist.rug.TestUtils
import com.atomist.source.ArtifactSource
import org.scalatest.{FlatSpec, Matchers}

class CSharpFileUsageTest extends FlatSpec with Matchers {

  import CSharpFileType._
  import CSharpFileTypeTest._

  val cSharpFileType = new CSharpFileType

  def doModify(tsFilename: String, as: ArtifactSource, params: Map[String, String] = Map()): ModificationAttempt = {
    val pe = TestUtils.editorInSideFile(this, tsFilename)
    pe.modify(as, SimpleProjectOperationArguments("", params))
  }


  def modifyCSharpAndReparseSuccessfully(tsFilename: String, as: ArtifactSource, params: Map[String, String] = Map()): ArtifactSource = {
    doModify(tsFilename, as, params) match {
      case sm: SuccessfulModification =>
        sm.result.allFiles
          .filter(_.name.endsWith(CSharpExtension))
          .map(cs => cSharpFileType.parseToRawNode(cs.content))
          .map(tree => tree.childNodes.nonEmpty)
        sm.result
    }
  }

  it should "enumerate imports in simple project" in {
    val r = doModify("ListImports.ts", HelloWorldSources)
    r match {
      case nmn: NoModificationNeeded =>
    }
  }

  it should "modify imports in single file" in pendingUntilFixed {
    val r = modifyCSharpAndReparseSuccessfully("ChangeImports.ts", HelloWorldSources)
    val f = r.findFile("hello.cs").get
    f.content.contains("newImport") should be(true)
  }

}
