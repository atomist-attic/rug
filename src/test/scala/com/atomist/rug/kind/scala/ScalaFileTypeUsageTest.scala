package com.atomist.rug.kind.scala

import com.atomist.project.edit.{NoModificationNeeded, SuccessfulModification}
import com.atomist.rug.kind.grammar.AbstractTypeUnderFileTest
import com.atomist.source.SimpleFileBasedArtifactSource
import com.atomist.tree.utils.TreeNodeUtils
import org.scalatest.DiagrammedAssertions._

/**
  * Tests for realistic Scala scenarios
  */
class ScalaFileTypeUsageTest extends AbstractTypeUnderFileTest {

  import ScalaFileTypeTest._

  override val typeBeingTested = new ScalaFileType

  it should "change exception catch ???" is pending

  it should "name a specified parameter" in {
    modify("NameParameter.ts", PythonTypeSources) match {
      case sm: SuccessfulModification =>
        val theFile = sm.result.findFile(Python3Source.path).get
        //println(theFile.content)
        theFile.content.contains("nodeNamingStrategy =") should be (true)
        validateResultContainsValidFiles(sm.result)
      case wtf => fail(s"Expected SuccessfulModification, not $wtf")
    }
  }

  val diagrammedAssertionsImport = "import org.scalatest.DiagrammedAssertions._"

  it should "upgrade to DiagrammedAssertions when needed using path expressions" in
    upgradeToDiagrammedAssertionsWhenNeeded("ImportDiagrammedAssertions.ts")

  it should "upgrade to DiagrammedAssertions when needed using ScalaHelper" in
    upgradeToDiagrammedAssertionsWhenNeeded("ImportAdder.ts")

  private def upgradeToDiagrammedAssertionsWhenNeeded(sideFile: String) {
    modify(sideFile, ScalaTestSources) match {
      case sm: SuccessfulModification =>
        val theFile = sm.result.findFile(OldStyleScalaTest.path).get
        //println(theFile.content)
        assert(theFile.content.contains(diagrammedAssertionsImport) === true)
        validateResultContainsValidFiles(sm.result)
      case wtf => fail(s"Expected SuccessfulModification, not $wtf")
    }
  }

  it should "not upgrade to DiagrammedAssertions when not needed" in
    notUpgradeToDiagrammedAssertionsWhenNotNeeded

  private def notUpgradeToDiagrammedAssertionsWhenNotNeeded {
    val testWithImportAlready = OldStyleScalaTest.withContent(diagrammedAssertionsImport + "\n" + OldStyleScalaTest.content)
    modify("ImportDiagrammedAssertions.ts", SimpleFileBasedArtifactSource(testWithImportAlready)) match {
      case _: NoModificationNeeded =>
        // OK
      case wtf => fail(s"Expected SuccessfulModification, not $wtf")
    }
  }

  it should "change a.equals(b)" in {
//    val tn = typeBeingTested.fileToRawNode(UsesDotEquals).get
//    println(TreeNodeUtils.toShorterString(tn, TreeNodeUtils.NameAndContentStringifier))

    modify("EqualsToSymbol.ts", UsesDotEqualsSources) match {
      case sm: SuccessfulModification =>
        val theFile = sm.result.findFile(UsesDotEquals.path).get
        //println(theFile.content)
        theFile.content.contains("==") should be (true)
        theFile.content.contains("equals") should be (false)
        validateResultContainsValidFiles(sm.result)
      case wtf => fail(s"Expected SuccessfulModification, not $wtf")
    }
  }

  it should "upgrade ScalaTest assertions" in {
    modify("UpgradeScalaTestAssertions.ts", ScalaTestSources) match {
      case sm: SuccessfulModification =>
        val theFile = sm.result.findFile(OldStyleScalaTest.path).get
        //println(theFile.content)
        theFile.content.contains("===") should be (true)
        validateResultContainsValidFiles(sm.result)
      case wtf => fail(s"Expected SuccessfulModification, not $wtf")
    }
  }

}

