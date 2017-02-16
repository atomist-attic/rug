package com.atomist.rug.kind.yml.path

import com.atomist.rug.kind.yml.AbstractYmlUsageTest
import com.atomist.rug.kind.yml.YmlUsageTestTargets.allAS

class YmlFileTypeUsageTest extends AbstractYmlUsageTest {

//  it should "get group value with no change with native Rug function" in {
//    val prog =
//      """
//        |editor YmlEdit
//        |
//        |with File f when path = "x.yml"
//        | with YmlFile y
//        |   do valueOf "group"
//      """.stripMargin
//    allAS.foreach(asChanges => runProgAndCheck(prog, asChanges._1, 0))
//  }

  it should "change group value with native Rug function" in {
    val prog =
      """
        |editor YmlEdit
        |
        |with File f when path = "x.yml"
        | with YmlFile x
        |   with group
        |     do update "Marx Brothers"
      """.stripMargin
    allAS.foreach(asChanges => runProgAndCheck(prog, asChanges._1, 1))
  }

}
