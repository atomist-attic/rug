package com.atomist.rug.kind.csharp

import com.atomist.project.edit.NoModificationNeeded
import com.atomist.rug.kind.grammar.AntlrRawFileTypeTest

class CSharpFileTypeUsageTest extends AntlrRawFileTypeTest {

  import CSharpFileTypeTest._

  override val typeBeingTested = new CSharpFileType

  it should "enumerate usings in simple project" in {
    val r = modify("ListImports.ts", HelloWorldSources)
    r match {
      case nmn: NoModificationNeeded =>
    }
  }

  it should "enumerate usings in simple project with ill-formed C#" in {
    modify("ListImports.ts", ProjectWithBogusCSharp.currentBackingObject) match {
      case nmn: NoModificationNeeded =>
    }
  }

  it should "modify using via path expression" in pendingUntilFixed {
    val r = modifyAndReparseSuccessfully("ChangeUsing.ts", HelloWorldSources)
    val f = r.findFile("src/hello.cs").get
    f.content.contains("newImportWithAVeryVeryLongName") should be(true)
  }

  it should "add using in single file" in pendingUntilFixed {
    val r = modifyAndReparseSuccessfully("AddImport.ts", HelloWorldSources)
    val f = r.findFile("src/hello.cs").get
    println(f.content)
    f.content.contains("using System;") should be(true)
    f.content.contains("using Thing;") should be(true)
  }

  it should "not add important if already present" in pendingUntilFixed {
    val r = modifyAndReparseSuccessfully("AddUsingUsingMethod.ts", HelloWorldSources)
    val f = r.findFile("src/hello.cs").get
    println(f.content)
    f.content.contains("using Thing;") should be(false)
    // It should occur once
    f.content.indexOf("using System;") should be(f.content.lastIndexOf("using System;"))
  }

  it should "add missing using using type" in pendingUntilFixed {
    val r = modifyAndReparseSuccessfully("AddUsingUsingMethod.ts", HelloWorldSources)
    val f = r.findFile("src/hello.cs").get
    println(f.content)
    f.content.contains("using System;") should be(true)
    f.content.contains("using Thing;") should be(true)
  }

}
