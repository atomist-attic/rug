package com.atomist.rug.kind.scala

import com.atomist.project.edit.SuccessfulModification
import com.atomist.rug.kind.grammar.AbstractTypeUnderFileTest

class ScalaFileTypeUsageTest extends AbstractTypeUnderFileTest {

  import ScalaFileTypeTest._

  override val typeBeingTested = new ScalaFileType

  it should "change exception catch ???" is pending

  it should "upgrade ScalaTest assertions" in pendingUntilFixed {

    modify("UpgradeScalaTestAssertions.ts", ScalaTestSources) match {
      case sm: SuccessfulModification =>
        val theFile = sm.result.findFile(ScalaTestSources.allFiles.head.path).get
        theFile.content.contains("====") should be (true)
      case wtf => fail(s"Expected SuccessfulModification, not $wtf")
    }
  }

}

