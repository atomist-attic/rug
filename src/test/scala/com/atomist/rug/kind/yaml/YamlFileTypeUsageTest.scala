package com.atomist.rug.kind.yaml

import com.atomist.project.edit.{NoModificationNeeded, SuccessfulModification}
import com.atomist.rug.kind.core.ProjectMutableView
import com.atomist.rug.kind.grammar.{AbstractTypeUnderFileTest, TypeUnderFile}
import com.atomist.source.{FileArtifact, SimpleFileBasedArtifactSource, StringFileArtifact}
import com.atomist.tree.UpdatableTreeNode
import com.atomist.tree.content.text.{PositionedTreeNode, TextTreeNodeLifecycle}
import com.atomist.util.lang.NashornTest

class YamlFileTypeUsageTest extends AbstractTypeUnderFileTest with AbstractYamlUsageTest {

  override protected def typeBeingTested: TypeUnderFile = new YamlFileType

  import YamlUsageTestTargets._

  it should "change scalar value value" in {
    val newValue = "Marx Brothers"
    val pe = "/*[@name='x.yml']/YamlFile()/group"
    allAS.foreach(asChanges => {
      val r = runProgAndCheck("EditPath.ts", asChanges._1, 1, Map("path" -> pe, "newValue" -> newValue))
      assert(r.findFile("x.yml").get.content == xYaml.replace("queen", newValue))
    })
  }

  it should "change scalar value late in document" in {
    val newValue = "earth"
    val pe = "/*[@name='x.yml']/YamlFile()/common"
    allAS.foreach(asChanges => {
      val r = runProgAndCheck("EditPath.ts", asChanges._1, 1, Map("path" -> pe, "newValue" -> newValue))
      assert(r.findFile("x.yml").get.content == xYaml.replace("everywhere", newValue))
    })
  }

  it should "change collection elements" in {
    allAS.foreach(asChanges => {
      val r = runProgAndCheck("Optimist.ts", asChanges._1, 1, Map())
      assert(r.findFile("x.yml").get.content == xYaml.replace("Death", "Life"))
    })
  }

  it should "update string in start YAML example" in {
    val pe = "//YamlFile()//*[@name='bill-to']/given/value"
    val newValue = "Christine"
    val r = runProgAndCheck("EditPath.ts",
      SimpleFileBasedArtifactSource(StringFileArtifact("x.yml", YamlOrgStart)), 1, Map("path" -> pe, "newValue" -> newValue))
    assert(r.findFile("x.yml").get.content == YamlOrgStart.replace("Chris", "Christine"))
  }

  it should "change raw string" in {
    modify("ChangeRaw.ts", singleAS) match {
      case sm: SuccessfulModification =>
        val theFile = sm.result.findFile("x.yml").get
        // println(theFile.content)
        assert(theFile.content === xYaml.replace("queen", "Jefferson Airplane"))
        validateResultContainsValidFiles(sm.result)
      case wtf => fail(s"Expected SuccessfulModification, not $wtf")
    }
  }

  it should "change quoted string" in {
    modify("ChangeQuoted.ts", singleAS) match {
      case sm: SuccessfulModification =>
        val theFile = sm.result.findFile("x.yml").get
        // println(theFile.content)
        assert(theFile.content === xYaml.replace("Bohemian Rhapsody", "White Rabbit"))
        validateResultContainsValidFiles(sm.result)
      case wtf => fail(s"Expected SuccessfulModification, not $wtf")
    }
  }

  it should "change > string" in {
    val oldComment =
      """>
        |    Late afternoon is best.
        |    Backup contact is Nancy
        |    Billsmer @ 338-4338.
        |""".stripMargin
    val newComment = "This is the new comment"
    val rawNewComment =
      s""">
         |    $newComment
         |""".stripMargin

    modify("ChangeGt.ts",
      SimpleFileBasedArtifactSource(StringFileArtifact("x.yml", YamlOrgStart)),
      Map("newComment" -> newComment)) match {
      case sm: SuccessfulModification =>
        val theFile = sm.result.findFile("x.yml").get
        assert(theFile.content === YamlOrgStart.replace(oldComment, rawNewComment))
        validateResultContainsValidFiles(sm.result)
      case wtf => fail(s"Expected SuccessfulModification, not $wtf")
    }
  }

  it should "change > string with multi-line string" in {
    val oldComment =
      """>
        |    Late afternoon is best.
        |    Backup contact is Nancy
        |    Billsmer @ 338-4338.
        |""".stripMargin
    val newComment =
      """|Early morning is best.
         |    Backup contact is Bob
         |    Billdad @ 338-4338.
         |""".stripMargin
    val rawNewComment =
      s""">
         |    $newComment
         |""".stripMargin

    modify("ChangeGt.ts",
      SimpleFileBasedArtifactSource(StringFileArtifact("x.yml", YamlOrgStart)),
      Map("newComment" -> newComment)) match {
      case sm: SuccessfulModification =>
        val theFile = sm.result.findFile("x.yml").get
        // println(theFile.content)
        assert(theFile.content === YamlOrgStart.replace(oldComment, rawNewComment))
        validateResultContainsValidFiles(sm.result)
      case wtf => fail(s"Expected SuccessfulModification, not $wtf")
    }
  }

  it should "change > string again with multi-line string" in {
    val oldComment =
      """>
        |    Late afternoon is best.
        |    Backup contact is Nancy
        |
        |
        |    Billsmer @ 338-4338.
        |""".stripMargin
    val newComment =
      """|Early morning is best.
         |    Backup contact is Bob
         |    Billdad @ 338-4338.
         |""".stripMargin
    val rawNewComment =
      s""">
         |    $newComment
         |""".stripMargin

    modify("ChangeGt.ts",
      SimpleFileBasedArtifactSource(StringFileArtifact("x.yml", YamlFolded)),
      Map("newComment" -> newComment)) match {
      case sm: SuccessfulModification =>
        val theFile = sm.result.findFile("x.yml").get
        // println(theFile.content)
        assert(theFile.content === YamlFolded.replace(oldComment, rawNewComment))
        validateResultContainsValidFiles(sm.result)
      case wtf => fail(s"Expected SuccessfulModification, not $wtf")
    }
  }

  it should "change >- string with multi-line string" in {
    val oldComment =
      """>-
        |    Late afternoon is best.
        |    Backup contact is Nancy
        |    Billsmer @ 338-4338.
        |""".stripMargin
    val newComment =
      """|Early morning is best.
         |    Backup contact is Bob
         |    Billdad @ 338-4338.
         |""".stripMargin
    val rawNewComment =
      s""">-
         |    $newComment
         |""".stripMargin

    modify("ChangeGtStrip.ts",
      SimpleFileBasedArtifactSource(StringFileArtifact("x.yml", YamlFoldedStrip)),
      Map("newComment" -> newComment)) match {
      case sm: SuccessfulModification =>
        val theFile = sm.result.findFile("x.yml").get
        // println(theFile.content)
        assert(theFile.content === YamlFoldedStrip.replace(oldComment, rawNewComment))
        validateResultContainsValidFiles(sm.result)
      case wtf => fail(s"Expected SuccessfulModification, not $wtf")
    }
  }

  it should "change >+ string with multi-line string" in {
    val oldComment =
      """>+
        |    Late afternoon is best.
        |    Backup contact is Nancy
        |
        |    Billsmer @ 338-4338.
        |
        |
        |
        |""".stripMargin
    val newComment =
      """|Early morning is best.
         |    Backup contact is Bob
         |    Billdad @ 338-4338.
         |""".stripMargin
    val rawNewComment =
      s""">+
         |    $newComment
         |""".stripMargin

    modify("ChangeGtKeep.ts",
      SimpleFileBasedArtifactSource(StringFileArtifact("x.yml", YamlFoldedKeep)),
      Map("newComment" -> newComment)) match {
      case sm: SuccessfulModification =>
        val theFile = sm.result.findFile("x.yml").get
        // println(theFile.content)
        assert(theFile.content === YamlFoldedKeep.replace(oldComment, rawNewComment))
        validateResultContainsValidFiles(sm.result)
      case wtf => fail(s"Expected SuccessfulModification, not $wtf")
    }
  }

  it should "change | string" in {
    val oldComment =
      """|
        #    Late afternoon is best.
        #    Backup contact is Nancy
        #
        #
        #    Billsmer @ 338-4338.
        #
        #
        #""".stripMargin('#')
    val newComment = "This is the new comment"
    val rawNewComment =
      s"""|
          #    $newComment
          #""".stripMargin('#')

    modify("ChangePipe.ts",
      SimpleFileBasedArtifactSource(StringFileArtifact("x.yml", YamlLiteralBlockScalar)),
      Map("newComment" -> newComment)) match {
      case sm: SuccessfulModification =>
        val theFile = sm.result.findFile("x.yml").get
        // println(theFile.content)
        assert(theFile.content === YamlLiteralBlockScalar.replace(oldComment, rawNewComment))
        validateResultContainsValidFiles(sm.result)
      case wtf => fail(s"Expected SuccessfulModification, not $wtf")
    }
  }

  it should "change | string with multi-line string" in {
    val oldComment =
      """|
        #    Late afternoon is best.
        #    Backup contact is Nancy
        #
        #
        #    Billsmer @ 338-4338.
        #
        #
        #""".stripMargin('#')
    val newComment =
      """#Early morning is best.
         #    Backup contact is Bob
         #    Billdad @ 338-4338.
         #""".stripMargin('#')
    val rawNewComment =
      s"""|
          #    $newComment
          #""".stripMargin('#')

    modify("ChangePipe.ts",
      SimpleFileBasedArtifactSource(StringFileArtifact("x.yml", YamlLiteralBlockScalar)),
      Map("newComment" -> newComment)) match {
      case sm: SuccessfulModification =>
        val theFile = sm.result.findFile("x.yml").get
        // println(theFile.content)
        assert(theFile.content === YamlLiteralBlockScalar.replace(oldComment, rawNewComment))
        validateResultContainsValidFiles(sm.result)
      case wtf => fail(s"Expected SuccessfulModification, not $wtf")
    }
  }

  it should "change |- string with multi-line string" in {
    val oldComment =
      """|-
        #    Late afternoon is best.
        #    Backup contact is Nancy
        #
        #
        #    Billsmer @ 338-4338.
        #
        #
        #
        #""".stripMargin('#')
    val newComment =
      """#Early morning is best.
         #    Backup contact is Bob
         #    Billdad @ 338-4338.
         #""".stripMargin('#')
    val rawNewComment =
      s"""|-
          #    $newComment
          #""".stripMargin('#')

    modify("ChangePipeStrip.ts",
      SimpleFileBasedArtifactSource(StringFileArtifact("x.yml", YamlLiteralStrip)),
      Map("newComment" -> newComment)) match {
      case sm: SuccessfulModification =>
        val theFile = sm.result.findFile("x.yml").get
        // println(theFile.content)
        assert(theFile.content === YamlLiteralStrip.replace(oldComment, rawNewComment))
        validateResultContainsValidFiles(sm.result)
      case wtf => fail(s"Expected SuccessfulModification, not $wtf")
    }
  }

  it should "change |+ string with multi-line string" in {
    val oldComment =
      """|+
        #    Late afternoon is best.
        #    Backup contact is Nancy
        #
        #
        #    Billsmer @ 338-4338.
        #
        #
        #
        #""".stripMargin('#')
    val newComment =
      """#Early morning is best.
         #    Backup contact is Bob
         #    Billdad @ 338-4338.
         #""".stripMargin('#')
    val rawNewComment =
      s"""|+
          #    $newComment
          #""".stripMargin('#')

    modify("ChangePipeKeep.ts",
      SimpleFileBasedArtifactSource(StringFileArtifact("x.yml", YamlLiteralKeep)),
      Map("newComment" -> newComment)) match {
      case sm: SuccessfulModification =>
        val theFile = sm.result.findFile("x.yml").get
        // println(theFile.content)
        assert(theFile.content === YamlLiteralKeep.replace(oldComment, rawNewComment))
        validateResultContainsValidFiles(sm.result)
      case wtf => fail(s"Expected SuccessfulModification, not $wtf")
    }
  }

  it should "update a key" in pendingUntilFixed {
    modify("UpdateKey.ts", singleAS) match {
      case sm: SuccessfulModification =>
        val theFile = sm.result.findFile("x.yml").get
        // println(theFile.content)
        assert(theFile.content === xYaml.replace("dependencies", "songs"))
        validateResultContainsValidFiles(sm.result)
      case wtf => fail(s"Expected SuccessfulModification, not $wtf")
    }
  }

  it should "add to sequence" in {
    modify("AddToSequence.ts", singleAS) match {
      case sm: SuccessfulModification =>
        val theFile = sm.result.findFile("x.yml").get
        // println(theFile.content)
        assert(theFile.content.contains("Killer Queen"))
        validateResultContainsValidFiles(sm.result)
      case wtf => fail(s"Expected SuccessfulModification, not $wtf")
    }
  }

  it should "add to nested sequence" in {
    modify("AddToNestedSequence.ts",
      new SimpleFileBasedArtifactSource("single", StringFileArtifact("x.yml", YamlNestedSeq))) match {
      case sm: SuccessfulModification =>
        val theFile = sm.result.findFile("x.yml").get
        // println(theFile.content)
        assert(theFile.content.contains("NAP500"))
        validateResultContainsValidFiles(sm.result)
      case wtf => fail(s"Expected SuccessfulModification, not $wtf")
    }
  }

  it should "add to deep nested sequence" in {
    modify("AddToDeepNestedSequence.ts",
      new SimpleFileBasedArtifactSource("single", StringFileArtifact("x.yml", YamlNestedSeq))) match {
      case sm: SuccessfulModification =>
        val theFile = sm.result.findFile("x.yml").get
        // println(theFile.content)
        assert(theFile.content.contains("Audio Principe Signature power cable"))
        validateResultContainsValidFiles(sm.result)
      case wtf => fail(s"Expected SuccessfulModification, not $wtf")
    }
  }

  it should "remove from sequence" in {
    modify("RemoveFromSequence.ts", singleAS) match {
      case sm: SuccessfulModification =>
        val theFile = sm.result.findFile("x.yml").get
        // println(theFile.content)
        assert(!theFile.content.contains("Sweet Lady"))
        validateResultContainsValidFiles(sm.result)
      case wtf => fail(s"Expected SuccessfulModification, not $wtf")
    }
  }

  it should "fail to remove non-existent element from sequence" in {
    modify("RemoveNonExistentElemFromSequence.ts", singleAS) match {
      case sm: SuccessfulModification => fail(s"Expected NoModificationNeeded, not $sm")
      case nom: NoModificationNeeded =>
      case _ => fail(s"Expected NoModificationNeeded")
    }
  }

  it should "remove from deep nested sequence" in {
    modify("RemoveFromDeepNestedSequence.ts",
      new SimpleFileBasedArtifactSource("single", StringFileArtifact("x.yml", YamlNestedSeq))) match {
      case sm: SuccessfulModification =>
        val theFile = sm.result.findFile("x.yml").get
        assert(theFile != null)
        val tn = parseAndPrepare(theFile)
        val nodes = evaluatePathExpression(tn, "/components/Amplifier/*[@name='future upgrades']/NAC82/*")
        assert(!nodes.map(_.nodeName).contains("Hicap"))
        validateResultContainsValidFiles(sm.result)
      case wtf => fail(s"Expected SuccessfulModification, not $wtf")
    }
  }
  def parseAndPrepare(fileArtifact: FileArtifact): UpdatableTreeNode = {
    val f = new ProjectMutableView(SimpleFileBasedArtifactSource(fileArtifact)).findFile(fileArtifact.path)
    val tn = typeBeingTested.fileToRawNode(f.currentBackingObject).get
    // println(TreeNodeUtils.toShorterString(tn, TreeNodeUtils.NameAndContentStringifier))
    TextTreeNodeLifecycle.makeWholeFileNodeReady("Yaml", PositionedTreeNode.fromParsedNode(tn), f)
  }

  it should "change multiple documents in one file" is pending
}
