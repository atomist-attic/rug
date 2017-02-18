package com.atomist.rug.kind.yml.path

import com.atomist.project.edit.SuccessfulModification
import com.atomist.rug.kind.grammar.{AbstractTypeUnderFileTest, TypeUnderFile}
import com.atomist.rug.kind.yml.{AbstractYmlUsageTest, YmlUsageTestTargets}
import com.atomist.source.{SimpleFileBasedArtifactSource, StringFileArtifact}

class YmlFileTypeUsageTest extends AbstractTypeUnderFileTest with AbstractYmlUsageTest  {

  override protected def typeBeingTested: TypeUnderFile = new YmlFileType

  import YmlUsageTestTargets._

  it should "change scalar value value" in {
    val newContent = "Marx Brothers"
    val prog =
      s"""
         |editor YmlEdit
         |
        |let group = $$(/*[@name='x.yml']/YmlFile()/group)
         |
        |with group
         |     do update "$newContent"
      """.stripMargin
    allAS.foreach(asChanges => {
      val r = runProgAndCheck(prog, asChanges._1, 1)
      assert(r.findFile("x.yml").get.content == YmlUsageTestTargets.xYml.replace("queen", newContent))
    })
  }

  it should "change scalar value late in document" in {
    val newContent = "earth"
    val prog =
      s"""
         |editor YmlEdit
         |
         |let group = $$(/*[@name='x.yml']/YmlFile()/common)
         |
         |with group
         |     do update "$newContent"
      """.stripMargin
    allAS.foreach(asChanges => {
      val r = runProgAndCheck(prog, asChanges._1, 1)
      assert(r.findFile("x.yml").get.content == YmlUsageTestTargets.xYml.replace("everywhere", newContent))
    })
  }

  it should "change collection elements" in {
    val prog =
      s"""
         |editor YmlEdit
         |
         |let group = $$(/*[@name='x.yml']/YmlFile()/dependencies/*)
         |
         |with group g
         |     do update { g.value().replace("Death", "Life") } # Capitals are only present in the dependencies
      """.stripMargin
    allAS.foreach(asChanges => {
      val r = runProgAndCheck(prog, asChanges._1, 1)
      assert(r.findFile("x.yml").get.content == YmlUsageTestTargets.xYml.replace("Death", "Life"))
    })
  }

  it should "update string in start YAML example" in {
    val prog =
      s"""
         |editor YmlEdit
         |
         |let billTo = $$(//YmlFile()//*[@name='bill-to']/given/value)
         |
         |with billTo begin
         |     do update "Christine"
         |end
      """.stripMargin
    val r = runProgAndCheck(prog,
      SimpleFileBasedArtifactSource(StringFileArtifact("x.yml", YmlUsageTestTargets.YamlOrgStart)),
      1)
    assert(r.findFile("x.yml").get.content == YmlUsageTestTargets.YamlOrgStart.replace("Chris", "Christine"))
  }

  it should "change raw string" in {
    modify("ChangeRaw.ts", singleAS) match {
      case sm: SuccessfulModification =>
        val theFile = sm.result.findFile("x.yml").get
        println(theFile.content)
        assert(theFile.content === xYml.replace("queen", "Jefferson Airplane"))
        validateResultContainsValidFiles(sm.result)
      case wtf => fail(s"Expected SuccessfulModification, not $wtf")
    }
  }

  it should "change quoted string" in {
    modify("ChangeQuoted.ts", singleAS) match {
      case sm: SuccessfulModification =>
        val theFile = sm.result.findFile("x.yml").get
        println(theFile.content)
        assert(theFile.content === xYml.replace("Bohemian Rhapsody", "White Rabbit"))
        validateResultContainsValidFiles(sm.result)
      case wtf => fail(s"Expected SuccessfulModification, not $wtf")
    }
  }

  it should "change | string" is pending

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
        println(theFile.content)
        assert(theFile.content === YamlOrgStart.replace(oldComment, rawNewComment))
        validateResultContainsValidFiles(sm.result)
      case wtf => fail(s"Expected SuccessfulModification, not $wtf")
    }
  }

  it should "change multiple documents in one file" is pending

}
