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
          .flatMap(cs => cSharpFileType.parseToRawNode(cs.content))
          .map(tree => tree.childNodes.nonEmpty)
        sm.result
    }
  }

  it should "enumerate usings in simple project" in {
    val r = doModify("ListImports.ts", HelloWorldSources)
    r match {
      case nmn: NoModificationNeeded =>
    }
  }

  it should "enumerate usings in simple project with ill-formed C#" in {
    val r = doModify("ListImports.ts", ProjectWithBogusCSharp.currentBackingObject)
    r match {
      case nmn: NoModificationNeeded =>
    }
  }

  it should "modify using in single file" in {
    val r = modifyCSharpAndReparseSuccessfully("ChangeImports.ts", HelloWorldSources)
    val f = r.findFile("src/hello.cs").get
    f.content.contains("newImport") should be(true)
  }

  it should "add using in single file" in {
    val r = modifyCSharpAndReparseSuccessfully("AddImport.ts", HelloWorldSources)
    val f = r.findFile("src/hello.cs").get
    println(f.content)
    f.content.contains("using System;") should be(true)
    f.content.contains("using Thing;") should be(true)
  }

}
