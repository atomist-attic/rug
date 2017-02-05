package com.atomist.rug.kind.csharp

import com.atomist.project.edit.{NoModificationNeeded, SuccessfulModification}
import com.atomist.rug.kind.grammar.AntlrRawFileTypeTest

class CSharpFileTypeUsageTest extends AntlrRawFileTypeTest {

  import CSharpFileTypeTest._

  override val typeBeingTested = new CSharpFileType

  it should "enumerate usings in simple project" in {
    val r = modify("ListImports.ts", HelloWorldSources)
    r match {
      case nmn: NoModificationNeeded =>
      case _ => ???
    }
  }

  it should "enumerate usings in simple project with ill-formed C#" in {
    modify("ListImports.ts", projectWithBogusCSharp.currentBackingObject) match {
      case nmn: NoModificationNeeded =>
      case _ => ???
    }
  }

  it should "modify using via path expression" in {
    val r = modifyAndReparseSuccessfully("ChangeUsing.ts", HelloWorldSources)
    val f = r.findFile("src/hello.cs").get
    f.content.contains("newImportWithAVeryVeryLongName") should be(true)
  }

  it should "add using and verify" in {
    val r = modifyAndReparseSuccessfully("AddImport.ts", HelloWorldSources)
    val f = r.findFile("src/hello.cs").get
    f.content.contains("using System;") should be(true)
    f.content.contains("using System.Linq;") should be(true)
  }

  it should "not add using if it's already present" in {
    modify("AddUsingUsingMethod.ts", HelloWorldSources,
      Map("packageName" -> "System")) match {
      case nmn: NoModificationNeeded =>
      case _ => ???
    }
  }

  it should "add missing using via type" in {
    val r = modifyAndReparseSuccessfully("AddUsingUsingMethod.ts", HelloWorldSources,
      Map("packageName" -> "Thing"))
    val f = r.findFile("src/hello.cs").get
    f.content.contains("using System;") should be(true)
    f.content.contains("using Thing;") should be(true)
  }

  it should "add using if no using present" is pending

  it should "change exception" in {
    val newExceptionType = "ThePlaneHasFlownIntoTheMountainException"
    modify("ChangeException.ts", ExceptionProject,
      Map("newException" -> newExceptionType)) match {
      case sm: SuccessfulModification =>
        val theFile = sm.result.findFile("src/exception.cs").get
        theFile.content should be (Exceptions.replace("IndexOutOfRangeException", newExceptionType))
      case wtf => fail(s"Expected SuccessModification, not $wtf")
    }
  }

}
