package com.atomist.rug.kind.scala

import com.atomist.project.edit.{NoModificationNeeded, SuccessfulModification}
import com.atomist.rug.kind.grammar.AbstractTypeUnderFileTest
import com.atomist.source.SimpleFileBasedArtifactSource

/**
  * Tests for realistic Scala scenarios
  */

class ScalaFileTypeUsageTest extends AbstractTypeUnderFileTest {

  import ScalaFileTypeTest._

  override val typeBeingTested = new ScalaFileType

  "Scala file type in use" should "name a specified parameter" in {
    modify("NameParameter.ts", PythonTypeSources) match {
      case sm: SuccessfulModification =>
        val theFile = sm.result.findFile(Python3Source.path).get
        // println(theFile.content)
        theFile.content.contains("nodeNamingStrategy =") shouldBe true
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
        // println(theFile.content)
        assert(theFile.content.contains(diagrammedAssertionsImport) === true)
        validateResultContainsValidFiles(sm.result)
      case wtf => fail(s"Expected SuccessfulModification, not $wtf")
    }
  }

  it should "not upgrade to DiagrammedAssertions when not needed using path expressions" in
    notUpgradeToDiagrammedAssertionsWhenNotNeeded("ImportDiagrammedAssertions.ts")

  it should "not upgrade to DiagrammedAssertions when not needed using ScalaHelper" in
    notUpgradeToDiagrammedAssertionsWhenNotNeeded("ImportAdder.ts")

  private def notUpgradeToDiagrammedAssertionsWhenNotNeeded(sideFile: String) {
    val testWithImportAlready = OldStyleScalaTest.withContent(diagrammedAssertionsImport + "\n" + OldStyleScalaTest.content)
    modify(sideFile, SimpleFileBasedArtifactSource(testWithImportAlready)) match {
      case _: NoModificationNeeded =>
      // OK
      case wtf => fail(s"Expected SuccessfulModification, not $wtf")
    }
  }

  it should "change a.equals(b)" in {
    // val tn = typeBeingTested.fileToRawNode(UsesDotEquals).get
    //println(TreeNodeUtils.toShorterString(tn, TreeNodeUtils.NameAndContentStringifier))
    modify("EqualsToSymbol.ts", UsesDotEqualsSources) match {
      case sm: SuccessfulModification =>
        val theFile = sm.result.findFile(UsesDotEquals.path).get
        // println(theFile.content)
        theFile.content.contains("==") shouldBe true
        theFile.content.contains("equals") shouldBe false
        validateResultContainsValidFiles(sm.result)
      case wtf => fail(s"Expected SuccessfulModification, not $wtf")
    }
  }

  it should "remove printlns" in {
    // val tn = typeBeingTested.fileToRawNode(UsesPrintlnsSource).get
    // println(TreeNodeUtils.toShorterString(tn, TreeNodeUtils.NameAndContentStringifier))
    modify("RemovePrintlns.ts", UsesPrintlnsSources) match {
      case sm: SuccessfulModification =>
        val theFile = sm.result.findFile(UsesPrintlnsSource.path).get
        // println(theFile.content)
        theFile.content.contains("println") shouldBe false
        validateResultContainsValidFiles(sm.result)
      case wtf => fail(s"Expected SuccessfulModification, not $wtf")
    }
  }

  it should "convert printlns to logging" in {
    // val tn = typeBeingTested.fileToRawNode(UsesPrintlnsSource).get
    // println(TreeNodeUtils.toShorterString(tn, TreeNodeUtils.NameAndContentStringifier))
    modify("ConvertPrintlnsToLogging.ts", UsesPrintlnsSources) match {
      case sm: SuccessfulModification =>
        val theFile = sm.result.findFile(UsesPrintlnsSource.path).get
        // println(theFile.content)
        theFile.content.contains("println") shouldBe false
        theFile.content.contains("logger.debug") shouldBe true
        theFile.content.contains("import org.slf4j.LoggerFactory") shouldBe true
        theFile.content.contains("private lazy val logger: Logger = Logger(LoggerFactory.getLogger(getClass.getName))") shouldBe true
        validateResultContainsValidFiles(sm.result)
      case wtf => fail(s"Expected SuccessfulModification, not $wtf")
    }
  }

  it should "upgrade ScalaTest assertions" in {
    modify("UpgradeScalaTestAssertions.ts", ScalaTestSources) match {
      case sm: SuccessfulModification =>
        val theFile = sm.result.findFile(OldStyleScalaTest.path).get
        // println(theFile.content)
        theFile.content.contains("===") shouldBe true
        validateResultContainsValidFiles(sm.result)
      case wtf => fail(s"Expected SuccessfulModification, not $wtf")
    }
  }

  it should "remove double spacing from Scala file" in {
    modify("RemoveDoubleSpacedLines.ts", UsesDoubleSpacedSources) match {
      case sm: SuccessfulModification =>
        val theFile = sm.result.findFile(DoubleSpacedSource.path).get
        // println(theFile.content)
        theFile.content shouldNot include regex "\n$\n$"
        validateResultContainsValidFiles(sm.result)
      case wtf => fail(s"Expected SuccessfulModification, not $wtf")
    }
  }
}
