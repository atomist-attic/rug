package com.atomist.rug.kind.yaml.path

import com.atomist.project.edit.SuccessfulModification
import com.atomist.rug.kind.grammar.{AbstractTypeUnderFileTest, TypeUnderFile}
import com.atomist.rug.kind.yaml.{AbstractYamlUsageTest, YamlUsageTestTargets}
import com.atomist.source.{SimpleFileBasedArtifactSource, StringFileArtifact}

class YamlFileTypeUsageTest extends AbstractTypeUnderFileTest with AbstractYamlUsageTest {

  override protected def typeBeingTested: TypeUnderFile = new YamlFileType

  import YamlUsageTestTargets._

  it should "change scalar value value" in {
    val newContent = "Marx Brothers"
    val prog =
      s"""
         |editor YamlEdit
         |
         |let group = $$(/*[@name='x.yml']/YamlFile()/group)
         |
         |with group
         |     do update "$newContent"
      """.stripMargin
    allAS.foreach(asChanges => {
      val r = runProgAndCheck(prog, asChanges._1, 1)
      assert(r.findFile("x.yml").get.content == YamlUsageTestTargets.xYaml.replace("queen", newContent))
    })
  }

  it should "change scalar value late in document" in {
    val newContent = "earth"
    val prog =
      s"""
         |editor YamlEdit
         |
         |let group = $$(/*[@name='x.yml']/YamlFile()/common)
         |
         |with group
         |     do update "$newContent"
      """.stripMargin
    allAS.foreach(asChanges => {
      val r = runProgAndCheck(prog, asChanges._1, 1)
      assert(r.findFile("x.yml").get.content == YamlUsageTestTargets.xYaml.replace("everywhere", newContent))
    })
  }

  it should "change collection elements" in {
    val prog =
      s"""
         |editor YamlEdit
         |
         |let group = $$(/*[@name='x.yml']/YamlFile()/dependencies/*)
         |
         |with group g
         |     do update { g.value().replace("Death", "Life") } # Capitals are only present in the dependencies
      """.stripMargin
    allAS.foreach(asChanges => {
      val r = runProgAndCheck(prog, asChanges._1, 1)
      assert(r.findFile("x.yml").get.content == YamlUsageTestTargets.xYaml.replace("Death", "Life"))
    })
  }

  it should "update string in start YAML example" in {
    val prog =
      s"""
         |editor YamlEdit
         |
         |let billTo = $$(//YamlFile()//*[@name='bill-to']/given/value)
         |
         |with billTo begin
         |     do update "Christine"
         |end
      """.stripMargin
    val r = runProgAndCheck(prog,
      SimpleFileBasedArtifactSource(StringFileArtifact("x.yml", YamlUsageTestTargets.YamlOrgStart)),
      1)
    assert(r.findFile("x.yml").get.content == YamlUsageTestTargets.YamlOrgStart.replace("Chris", "Christine"))
  }

  it should "change raw string" in {
    modify("ChangeRaw.ts", singleAS) match {
      case sm: SuccessfulModification =>
        val theFile = sm.result.findFile("x.yml").get
        println(theFile.content)
        assert(theFile.content === xYaml.replace("queen", "Jefferson Airplane"))
        validateResultContainsValidFiles(sm.result)
      case wtf => fail(s"Expected SuccessfulModification, not $wtf")
    }
  }

  it should "change quoted string" in {
    modify("ChangeQuoted.ts", singleAS) match {
      case sm: SuccessfulModification =>
        val theFile = sm.result.findFile("x.yml").get
        println(theFile.content)
        assert(theFile.content === xYaml.replace("Bohemian Rhapsody", "White Rabbit"))
        validateResultContainsValidFiles(sm.result)
      case wtf => fail(s"Expected SuccessfulModification, not $wtf")
    }
  }

  it should "change | string" in pendingUntilFixed {
    val oldComment =
      """|
        #    Late afternoon is best.
        #    Backup contact is Nancy
        #    Billsmer @ 338-4338.""".stripMargin('#')
    val newComment = "This is the new comment"
    val rawNewComment =
      s"""|
         #    $newComment""".stripMargin('#')

    modify("ChangePipe.ts",
      SimpleFileBasedArtifactSource(StringFileArtifact("x.yml", YamlOrgStart2)),
      Map("newComment" -> newComment)) match {
      case sm: SuccessfulModification =>
        val theFile = sm.result.findFile("x.yml").get
        // println(theFile.content)
        assert(theFile.content === YamlOrgStart2.replace(oldComment, rawNewComment))
        validateResultContainsValidFiles(sm.result)
      case wtf => fail(s"Expected SuccessfulModification, not $wtf")
    }
  }

  it should "change > string" in pendingUntilFixed {
    val oldComment =
      """>
        |    Late afternoon is best.
        |    Backup contact is Nancy
        |    Billsmer @ 338-4338.""".stripMargin
    val newComment = "This is the new comment"
    val rawNewComment =
      s""">
         |    $newComment""".stripMargin

    modify("ChangeGt.ts",
      SimpleFileBasedArtifactSource(StringFileArtifact("x.yml", YamlOrgStart)),
      Map("newComment" -> newComment)) match {
      case sm: SuccessfulModification =>
        val theFile = sm.result.findFile("x.yml").get
        println(rawNewComment)
        println("1 " + theFile.content)
        println("2 " + YamlOrgStart.replace(oldComment, rawNewComment))
        assert(theFile.content === YamlOrgStart.replace(oldComment, rawNewComment))
        validateResultContainsValidFiles(sm.result)
      case wtf => fail(s"Expected SuccessfulModification, not $wtf")
    }
  }

  it should "change > string with multi-line string" in pendingUntilFixed {
    val oldComment =
      """>
        |    Late afternoon is best.
        |    Backup contact is Nancy
        |    Billsmer @ 338-4338.""".stripMargin
    val newComment =
      """|Early morning is best.
         |    Backup contact is Bob
         |    Billdad @ 338-4338.""".stripMargin
    val rawNewComment =
      s""">
         |    $newComment""".stripMargin

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

  it should "add to sequence" in pendingUntilFixed {
    modify("AddToSequence.ts", singleAS) match {
      case sm: SuccessfulModification =>
        val theFile = sm.result.findFile("x.yml").get
        println(theFile.content)
        // assert(theFile.content === xYaml.replace("queen", "Jefferson Airplane"))
        assert(theFile.content.contains("Not There"))
        validateResultContainsValidFiles(sm.result)
      case wtf => fail(s"Expected SuccessfulModification, not $wtf")
    }
  }

  it should "remove from sequence" is pending

  it should "change multiple documents in one file" is pending
}
