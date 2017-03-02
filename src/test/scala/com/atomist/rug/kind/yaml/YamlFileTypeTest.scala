package com.atomist.rug.kind.yaml

import com.atomist.rug.kind.core.ProjectMutableView
import com.atomist.rug.kind.grammar.{AbstractTypeUnderFileTest, TypeUnderFile}
import com.atomist.rug.kind.yaml.YamlUsageTestTargets._
import com.atomist.source.{SimpleFileBasedArtifactSource, StringFileArtifact}
import com.atomist.tree.utils.NodeUtils
import com.atomist.tree.{TreeNode, UpdatableTreeNode}

class YamlFileTypeTest extends AbstractTypeUnderFileTest {

  override protected def typeBeingTested: TypeUnderFile = new YamlFileType

  it should "parse and run path expression to find sequence" in {
    val f = StringFileArtifact("test.yml", YamlNestedSeq)
    val tn = typeBeingTested.fileToRawNode(f).get
    // println(TreeNodeUtils.toShorterString(tn, TreeNodeUtils.NameAndContentStringifier))

    val nodes = evaluatePathExpression(tn, "/components/*")
    assert(NodeUtils.value(nodes.last) === "Nait 3R")
  }

  it should "parse and run path expression to find nested sequence" in {
    val f = StringFileArtifact("test.yml", YamlNestedSeq)
    val tn = typeBeingTested.fileToRawNode(f).get
    // println(TreeNodeUtils.toShorterString(tn, TreeNodeUtils.NameAndContentStringifier))

    val nodes = evaluatePathExpression(tn, "/components/cables/*")
    assert(NodeUtils.value(nodes.last) === "A5 speaker cable")
  }

  it should "parse and run path expression to find deeper nested sequence" in {
    val f = StringFileArtifact("test.yml", YamlNestedSeq)
    val tn = typeBeingTested.fileToRawNode(f).get
    // println(TreeNodeUtils.toShorterString(tn, TreeNodeUtils.NameAndContentStringifier))

    val nodes = evaluatePathExpression(tn, "/components/Amplifier/*[@name='future upgrades']/*[@value='NAP250.2']")
    assert(nodes.size === 1)
    assert(NodeUtils.value(nodes.last) === "NAP250.2")
  }

  it should "parse and run path expression to find deepest nested sequence" in {
    val f = StringFileArtifact("test.yml", YamlNestedSeq)
    val tn = typeBeingTested.fileToRawNode(f).get
    // println(TreeNodeUtils.toShorterString(tn, TreeNodeUtils.NameAndContentStringifier))

    val nodes = evaluatePathExpression(tn, "/components/Amplifier/*[@name='future upgrades']/NAC82/*")
    assert(NodeUtils.value(nodes.head) === "NAPSC power supply")
  }

  it should "parse and output unchanged" in {
    val f = StringFileArtifact("test.yml", xYaml)
    val tn = typeBeingTested.fileToRawNode(f).get
    // println(TreeNodeUtils.toShorterString(tn, TreeNodeUtils.NameAndContentStringifier))

    withClue(s"Was [${tn.value}]\nExpected [${f.content}]") {
      assert(tn.value === f.content)
    }
  }

  it should "find scala value in quotes" in {
    val f = StringFileArtifact("test.yml", xYaml)
    val tn = typeBeingTested.fileToRawNode(f).get
    // println(TreeNodeUtils.toShorterString(tn, TreeNodeUtils.NameAndContentStringifier))

    val nodes = evaluatePathExpression(tn, "/artifact")
    assert(nodes.size == 1)
    assert(NodeUtils.value(nodes.head) === "\"A Night at the Opera\"")
  }

  it should "find scala value in quotes and modify" in {
    val f = StringFileArtifact("test.yml", xYaml)
    val pmv = new ProjectMutableView(SimpleFileBasedArtifactSource(f))
    val found = typeBeingTested.findAllIn(pmv)
    assert(found.get.size === 1)
    val tn = found.get.head
    // println(TreeNodeUtils.toShorterString(tn, TreeNodeUtils.NameAndContentStringifier))
    val oldContent = "\"A Night at the Opera\""

    val nodes = evaluatePathExpression(tn, "/artifact")
    assert(nodes.size == 1)
    assert(nodes.head.asInstanceOf[TreeNode].value === oldContent)

    val newContent = "What in God's holy name are you blathering about?"
    nodes.head.asInstanceOf[UpdatableTreeNode].update(newContent)
    assert(pmv.findFile("test.yml").content === xYaml.replace(oldContent, newContent))
  }

  it should "find scalar using path expression key" in {
    val f = StringFileArtifact("test.yml", xYaml)
    val tn = typeBeingTested.fileToRawNode(f).get

    var nodes = evaluatePathExpression(tn, "/group")
    assert(nodes.size == 1)
    assert(nodes.head.asInstanceOf[TreeNode].value === "queen")

    nodes = evaluatePathExpression(tn, "/group/value")
    assert(nodes.size == 1)
    assert(nodes.head.asInstanceOf[TreeNode].value === "queen")

    nodes = evaluatePathExpression(tn, "/group/value[@value='queen']")
    assert(nodes.size == 1)
    assert(nodes.head.asInstanceOf[TreeNode].value === "queen")
  }

  it should "parse and run path expression using name again" in {
    val f = StringFileArtifact("test.yml", xYaml)
    val tn = typeBeingTested.fileToRawNode(f).get
    // println(TreeNodeUtils.toShorterString(tn, TreeNodeUtils.NameAndContentStringifier))

    val nodes = evaluatePathExpression(tn, "/dependencies")
    assert(nodes.size == 1)
    val nodes2 = evaluatePathExpression(tn, "/dependencies/*")
    assert(nodes2.size === 12)
    assert(NodeUtils.value(nodes2.last) === "\"God Save the Queen\"")
  }

  it should "parse and run path expression using type" in {
    val f = StringFileArtifact("test.yml", xYaml)
    val tn = typeBeingTested.fileToRawNode(f).get
    // println(TreeNodeUtils.toShorterString(tn, TreeNodeUtils.NameAndContentStringifier))

    val nodes2 = evaluatePathExpression(tn, "/Sequence()[@name='dependencies']/*")
    assert(nodes2.size === 12)
    assert(NodeUtils.value(nodes2.last) === "\"God Save the Queen\"")
  }

  it should "parse and run path expression against YamlOrgStart invoice" in pendingUntilFixed {
    val f = StringFileArtifact("test.yml", YamlOrgStart)
    val tn = typeBeingTested.fileToRawNode(f).get
    // println(TreeNodeUtils.toShorterString(tn, TreeNodeUtils.NameAndContentStringifier))

    val nodes = evaluatePathExpression(tn, "//bill-to/given")
    assert(nodes.size == 1)
  }

  it should "parse and run path expression using | and > strings" in {
    val f = StringFileArtifact("test.yml", YamlOrgStart)
    val tn = typeBeingTested.fileToRawNode(f).get
    assert(tn.value === f.content)
    val nodes = evaluatePathExpression(tn, "//*[@name='bill-to']/given/value")
    assert(nodes.size === 1)
    assert(nodes.head.asInstanceOf[TreeNode].value === "Chris")
  }

  it should "return correct value for | string" in {
    val f = StringFileArtifact("test.yml", YamlOrgStart)
    val tn = typeBeingTested.fileToRawNode(f).get
    // println(TreeNodeUtils.toShorterString(tn, TreeNodeUtils.NameAndContentStringifier))

    val nodes = evaluatePathExpression(tn, "//*[@name='bill-to']/address/lines")
    assert(nodes.size === 1)
    val target = nodes.head
    // assert(nodes.head.asInstanceOf[TreeNode].value === "Chris")
    // Should have stripped whitespace
    // assert(target.value === "458 Walkman Dr.\nSuite #292\n")
  }

  it should "return correct value for > string" in {
    val f = StringFileArtifact("test.yml", YamlOrgStart)
    val tn = typeBeingTested.fileToRawNode(f).get
    // println(TreeNodeUtils.toShorterString(tn, TreeNodeUtils.NameAndContentStringifier))

    val nodes = evaluatePathExpression(tn, "/comments")
    assert(nodes.size === 1)
    val target = nodes.head
    // println(TreeNodeUtils.toShorterString(nodes.head, TreeNodeUtils.NameAndContentStringifier))

    // TODO define this behavior
    //    val expected =
    //      """
    //        |Late afternoon is best.
    //        |Backup contact is Nancy
    //        |Billsmer @ 338-4338.
    //      """.stripMargin
    //    // Should strip whitespace
    //    assert(target.value === expected)
  }

  it should "parse multiple documents in one file" is pending
}
