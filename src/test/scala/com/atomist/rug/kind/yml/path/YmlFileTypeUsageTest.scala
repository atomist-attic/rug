package com.atomist.rug.kind.yml.path

import com.atomist.rug.kind.yml.{AbstractYmlUsageTest, YmlUsageTestTargets}
import com.atomist.rug.kind.yml.YmlUsageTestTargets.allAS

class YmlFileTypeUsageTest extends AbstractYmlUsageTest {

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

}
