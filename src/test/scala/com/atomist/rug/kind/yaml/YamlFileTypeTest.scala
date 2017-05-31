package com.atomist.rug.kind.yaml

import com.atomist.rug.kind.core.ProjectMutableView
import com.atomist.rug.kind.grammar.{AbstractTypeUnderFileTest, TypeUnderFile}
import com.atomist.rug.kind.yaml.YamlUsageTestTargets._
import com.atomist.source.{SimpleFileBasedArtifactSource, StringFileArtifact}
import com.atomist.tree.content.text.{OverwritableTextTreeNode, PositionedTreeNode, TextTreeNodeLifecycle}
import com.atomist.tree.utils.{NodeUtils, TreeNodePrinter}
import com.atomist.tree.{TreeNode, UpdatableTreeNode}

class YamlFileTypeTest extends AbstractTypeUnderFileTest {

  override protected def typeBeingTested: TypeUnderFile = new YamlFileType

  def parseAndPrepare(fileContent: String = YamlNestedSeq): UpdatableTreeNode = {
    val f = new ProjectMutableView(SimpleFileBasedArtifactSource(StringFileArtifact("test.yml", fileContent))).findFile("test.yml")
    val tn = typeBeingTested.fileToRawNode(f.currentBackingObject).get
    // println(TreeNodeUtils.toShorterString(tn, TreeNodeUtils.NameAndContentStringifier))
    TextTreeNodeLifecycle.makeWholeFileNodeReady("Yaml", PositionedTreeNode.fromParsedNode(tn), f)
  }

  "yaml file type" should "parse and run path expression to find sequence" in {
    val tn = parseAndPrepare(YamlNestedSeq)
    // println(TreeNodeUtils.toShorterString(tn, TreeNodeUtils.NameAndContentStringifier))
    val nodes = evaluatePathExpression(tn, "/components/*")
    assert(nodes.last.asInstanceOf[OverwritableTextTreeNode].value === "Nait 3R")
  }

  it should "parse and run path expression to find nested sequence" in {
    val tn = parseAndPrepare(YamlNestedSeq)
    // println(TreeNodeUtils.toShorterString(tn, TreeNodeUtils.NameAndContentStringifier))
    val nodes = evaluatePathExpression(tn, "/components/cables/*")
    assert(nodes.last.asInstanceOf[OverwritableTextTreeNode].value === "A5 speaker cable")
  }

  it should "parse and run path expression to find deeper nested sequence" in {
    val tn = parseAndPrepare()
    val nodes = evaluatePathExpression(tn, "/components/Amplifier/*[@name='future upgrades']/*[@value='NAP250.2']")
    assert(nodes.size === 1)
    assert(NodeUtils.value(nodes.last) === "NAP250.2")
  }


  it should "parse and run path expression to find deepest nested sequence" in {
    val otif = parseAndPrepare()

    val nodes = evaluatePathExpression(otif, "/components/Amplifier/*[@name='future upgrades']/NAC82/*")
    assert(NodeUtils.value(nodes.head) === "NAPSC power supply")
  }

  it should "parse and output unchanged" in {
    val f = StringFileArtifact("test.yml", xYaml)
    val tn = typeBeingTested.fileToRawNode(f).get
    // println(TreeNodeUtils.toShorterString(tn, TreeNodeUtils.NameAndContentStringifier))
    val nodeValue = NodeUtils.positionedValue(tn, xYaml)
    withClue(s"Was [${nodeValue}]\nExpected [${f.content}]") {
      assert(nodeValue === f.content)
    }
  }

  it should "find scala value in quotes" in {
    val tn = parseAndPrepare(xYaml)
    // println(TreeNodeUtils.toShorterString(tn, TreeNodeUtils.NameAndContentStringifier))

    val nodes = evaluatePathExpression(tn, "/artifact")
    assert(nodes.size == 1)
    assert(nodes.last.asInstanceOf[OverwritableTextTreeNode].value === "\"A Night at the Opera\"")
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

  def printTree(t: UpdatableTreeNode) =
    println(TreeNodePrinter.draw[UpdatableTreeNode](u => u.childNodes.map(_.asInstanceOf[UpdatableTreeNode]), u => s"${u.nodeName}: ${u.value}")(t))

  it should "find scalar using path expression key" in {
    val tn = parseAndPrepare(xYaml)

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
    val tn = parseAndPrepare(xYaml)
    // println(TreeNodeUtils.toShorterString(tn, TreeNodeUtils.NameAndContentStringifier))

    val nodes = evaluatePathExpression(tn, "/dependencies")
    assert(nodes.size == 1)
    val nodes2 = evaluatePathExpression(tn, "/dependencies/*")
    assert(nodes2.size === 12)
    assert(NodeUtils.value(nodes2.last) === "\"God Save the Queen\"")
  }

  it should "parse and run path expression using type" in {
    val tn = parseAndPrepare(xYaml)
    // println(TreeNodeUtils.toShorterString(tn, TreeNodeUtils.NameAndContentStringifier))

    val nodes2 = evaluatePathExpression(tn, "/Sequence()[@name='dependencies']/*")
    assert(nodes2.size === 12)
    assert(NodeUtils.value(nodes2.last) === "\"God Save the Queen\"")
  }

  it should "parse and run path expression against YamlOrgStart invoice" in {
    val tn = parseAndPrepare(YamlOrgStart)
    val nodes = evaluatePathExpression(tn, "//bill-to/given")
    assert(nodes.size == 1)
  }

  it should "parse and run path expression using | and > strings" in {
    val tn = parseAndPrepare(YamlOrgStart)
    assert(tn.value === YamlOrgStart)
    val nodes = evaluatePathExpression(tn, "//*[@name='bill-to']/given/value")
    assert(nodes.size === 1)
    assert(nodes.head.asInstanceOf[TreeNode].value === "Chris")
  }

  it should "return correct value for | string" in {
    val tn = parseAndPrepare(YamlOrgStart)
    val nodes = evaluatePathExpression(tn, "//*[@name='bill-to']/address/lines")
    assert(nodes.size === 1)
    val target = nodes.head
    // assert(nodes.head.asInstanceOf[TreeNode].value === "Chris")
    // Should have stripped whitespace
    // assert(target.value === "458 Walkman Dr.\nSuite #292\n")
  }

  it should "return correct value for > string" in {
    val tn = parseAndPrepare(YamlOrgStart)
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
