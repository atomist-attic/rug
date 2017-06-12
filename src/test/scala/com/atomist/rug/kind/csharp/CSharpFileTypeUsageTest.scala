package com.atomist.rug.kind.csharp

import com.atomist.project.edit.{NoModificationNeeded, SuccessfulModification}
import com.atomist.rug.kind.grammar.AbstractTypeUnderFileTest

class CSharpFileTypeUsageTest extends AbstractTypeUnderFileTest {

  import CSharpFileTypeTest._

  override val typeBeingTested = new CSharpFileType

  "C# support" should "enumerate usings in simple project" in {
    val r = modify("ListImports.ts", HelloWorldSources)
    r match {
      case _: NoModificationNeeded =>
      case wtf => fail(s"Expected NoModificationNeeded, not $wtf")
    }
  }

  it should "enumerate usings in simple project with ill-formed C#" in {
    modify("ListImports.ts", projectWithBogusCSharp.currentBackingObject) match {
      case _: NoModificationNeeded =>
      case wtf => fail(s"Expected NoModificationNeeded, not $wtf")
    }
  }

  it should "modify using via path expression" in {
    val r = modifyAndReparseSuccessfully("ChangeUsing.ts", HelloWorldSources)
    val f = r.findFile("src/hello.cs").get
    f.content.contains("newImportWithAVeryVeryLongName") shouldBe true
  }

  it should "add using and verify" in {
    val r = modifyAndReparseSuccessfully("AddUsing.ts", HelloWorldSources)
    val f = r.findFile("src/hello.cs").get
    f.content.contains("using System;") shouldBe true
    f.content.contains("using System.Linq;") shouldBe true
  }

  it should "add using if no using present" is pending

  it should "change exception, validating navigating up and down target nodes" in {
    val newExceptionType = "ThePlaneHasFlownIntoTheMountainException"
    modify("ChangeException.ts", exceptionProject,
      Map("newException" -> newExceptionType)) match {
      case sm: SuccessfulModification =>
        val theFile = sm.result.findFile("src/exception.cs").get
        assert(theFile.content === Exceptions.replace("IndexOutOfRangeException", newExceptionType))
      case wtf =>
        fail(s"Expected SuccessfulModification, not $wtf")
    }
  }

  it should "navigate up and down tree with TypeScript helper" in {
    modify("NavigateTree.ts", exceptionProject) match {
      case _: NoModificationNeeded =>
      case wtf => fail(s"Expected NoModificationNeeded, not $wtf")
    }
  }

}
