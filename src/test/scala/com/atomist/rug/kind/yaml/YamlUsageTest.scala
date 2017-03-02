package com.atomist.rug.kind.yaml

// TODO commented out because of the Rug DSL
// and because this type may be end of lifed
// If the type survives, restore and convert
abstract class YamlUsageTest extends AbstractYamlUsageTest {

  import YamlUsageTestTargets._

  it should "change group value with native Rug function" in {
    val prog =
      """
        |editor YamlEdit
        |
        |with Yml x when path = "x.yml"
        |  do updateKey "group" "Marx Brothers"
      """.stripMargin
    allAS.foreach(asChanges => runProgAndCheck(prog, asChanges._1, 1))
  }

  it should "change group value only if exists with native Rug function" in {
    val prog =
      """
        |editor YamlEdit
        |
        |with Yml x
        |  do updateKey "group" "Marx Brothers"
      """.stripMargin
    allAS.foreach(asChanges => runProgAndCheck(prog, asChanges._1, 1))
  }

  it should "change group value in all files with native Rug function" in {
    val prog =
      """
        |editor YamlEdit
        |
        |with Yml x
        |  do updateKey "common" "Be"
      """.stripMargin
    allAS.foreach(asChanges => runProgAndCheck(prog, asChanges._1, asChanges._2))
  }

  it should "get group value via tree expression with no change with native Rug function" in {
    val prog =
      """
        |editor YamlEdit
        |
        |let pe = $(/*[@name='x.yml']/Yml())
        |
        |with pe
        |  do valueOf "group"
      """.stripMargin
    allAS.foreach(asChanges => runProgAndCheck(prog, asChanges._1, 0))
  }

  it should "change group value via tree expression with native Rug function" in {
    val prog =
      """
        |editor YamlEdit
        |
        |let pe = $(/*[@name='x.yml']/Yml())
        |
        |with pe
        |  do updateKey "group" "Marx Brothers"
      """.stripMargin
    allAS.foreach(asChanges => runProgAndCheck(prog, asChanges._1, 1))
  }

  it should "change group value via tree expression only if exists with native Rug function" in {
    val prog =
      """
        |editor YamlEdit
        |
        |let pe = $(/Yml())
        |
        |with pe
        |  do updateKey "group" "Marx Brothers"
      """.stripMargin
    allAS.foreach(asChanges => runProgAndCheck(prog, asChanges._1, 1))
  }

  it should "change group value via tree expression in all files with native Rug function" in {
    val prog =
      """
        |editor YamlEdit
        |
        |let pe = $(/Yml())
        |
        |with pe
        |  do updateKey "common" "Be"
      """.stripMargin
    allAS.foreach(asChanges => runProgAndCheck(prog, asChanges._1, asChanges._2))
  }

  it should "make no changes when tree expression does not match any files" in {
    val prog =
      """
        |editor YamlEdit
        |
        |let pe = $(/src/*[@name='x.yml']/Yml())
        |
        |with pe
        |  do updateKey "common" "Be"
      """.stripMargin
    allAS.foreach(asChanges => runProgAndCheck(prog, asChanges._1, 0))
  }
}
