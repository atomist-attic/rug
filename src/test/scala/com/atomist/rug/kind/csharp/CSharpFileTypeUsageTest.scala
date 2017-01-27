package com.atomist.rug.kind.csharp

import com.atomist.project.SimpleProjectOperationArguments
import com.atomist.project.edit.{ModificationAttempt, NoModificationNeeded, SuccessfulModification}
import com.atomist.rug.TestUtils
import com.atomist.rug.kind.grammar.AntlrRawFileTypeTest
import com.atomist.source.ArtifactSource

class CSharpFileTypeUsageTest extends AntlrRawFileTypeTest {

  import CSharpFileTypeTest._

  override val typeBeingTested = new CSharpFileType

  def doModify(tsFilename: String, as: ArtifactSource, params: Map[String, String] = Map()): ModificationAttempt = {
    val pe = TestUtils.editorInSideFile(this, tsFilename)
    pe.modify(as, SimpleProjectOperationArguments("", params))
  }


  // TODO pull the parsing part out into a common class
  def modifyCSharpAndReparseSuccessfully(tsFilename: String, as: ArtifactSource, params: Map[String, String] = Map()): ArtifactSource = {
    doModify(tsFilename, as, params) match {
      case sm: SuccessfulModification =>
        validateResultContainsValidFiles(sm.result)
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
    doModify("ListImports.ts", ProjectWithBogusCSharp.currentBackingObject) match {
      case nmn: NoModificationNeeded =>
    }
  }

  it should "modify using in single file" in pendingUntilFixed {
    val r = modifyCSharpAndReparseSuccessfully("ChangeImports.ts", HelloWorldSources)
    val f = r.findFile("src/hello.cs").get
    f.content.contains("newImportWithAVeryVeryLongName") should be(true)
  }

  it should "add using in single file" in pendingUntilFixed {
    val r = modifyCSharpAndReparseSuccessfully("AddImport.ts", HelloWorldSources)
    val f = r.findFile("src/hello.cs").get
    println(f.content)
    f.content.contains("using System;") should be(true)
    f.content.contains("using Thing;") should be(true)
  }

  it should "not add important if already present" in pendingUntilFixed {
    val r = modifyCSharpAndReparseSuccessfully("AddUsingUsingMethod.ts", HelloWorldSources)
    val f = r.findFile("src/hello.cs").get
    println(f.content)
    f.content.contains("using Thing;") should be(false)
    // It should occur once
    f.content.indexOf("using System;") should be(f.content.lastIndexOf("using System;"))
  }

  it should "add missing using using type" in pendingUntilFixed {
    val r = modifyCSharpAndReparseSuccessfully("AddUsingUsingMethod.ts", HelloWorldSources)
    val f = r.findFile("src/hello.cs").get
    println(f.content)
    f.content.contains("using System;") should be(true)
    f.content.contains("using Thing;") should be(true)
  }

}
