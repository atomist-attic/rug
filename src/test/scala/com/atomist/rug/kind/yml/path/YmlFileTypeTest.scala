package com.atomist.rug.kind.yml.path

import com.atomist.rug.kind.core.ProjectMutableView
import com.atomist.rug.kind.grammar.{AbstractTypeUnderFileTest, TypeUnderFile}
import com.atomist.rug.kind.yml.YmlUsageTestTargets
import com.atomist.source.{SimpleFileBasedArtifactSource, StringFileArtifact}
import com.atomist.tree.{MutableTreeNode, TreeNode, UpdatableTreeNode}
import com.atomist.tree.content.text.OverwritableTextTreeNode
import com.atomist.tree.utils.{NodeUtils, TreeNodeUtils}

class YmlFileTypeTest extends AbstractTypeUnderFileTest {

  override protected def typeBeingTested: TypeUnderFile = new YmlFileType

  it should "parse and output unchanged" in {
    val f = StringFileArtifact("test.yml", YmlUsageTestTargets.xYml)
    val tn = typeBeingTested.fileToRawNode(f).get
    //println(TreeNodeUtils.toShorterString(tn, TreeNodeUtils.NameAndContentStringifier))

    withClue(s"Was [${tn.value}]\nExpected [${f.content}]") {
      assert(tn.value === f.content)
    }
  }

  it should "find scala value in quotes" in {
    val f = StringFileArtifact("test.yml", YmlUsageTestTargets.xYml)
    val tn = typeBeingTested.fileToRawNode(f).get
    //println(TreeNodeUtils.toShorterString(tn, TreeNodeUtils.NameAndContentStringifier))

    val nodes = evaluatePathExpression(tn, "/artifact")
    assert(nodes.size == 1)
    assert(NodeUtils.value(nodes.head) === "A Night at the Opera")
  }

  it should "find scala value in quotes and modify" in {
    val f = StringFileArtifact("test.yml", YmlUsageTestTargets.xYml)
    val pmv = new ProjectMutableView(SimpleFileBasedArtifactSource(f))
    val found = typeBeingTested.findAllIn(pmv)
    assert(found.get.size === 1)
    val tn = found.get.head
    //println(TreeNodeUtils.toShorterString(tn, TreeNodeUtils.NameAndContentStringifier))
    val oldContent = "A Night at the Opera"

    val nodes = evaluatePathExpression(tn, "/artifact")
    assert(nodes.size == 1)
    assert(nodes.head.asInstanceOf[TreeNode].value === oldContent)

    val newContent = "What in God's holy name are you blathering about?"
    nodes.head.asInstanceOf[UpdatableTreeNode].update(newContent)
    assert(pmv.findFile("test.yml").content === YmlUsageTestTargets.xYml.replace(oldContent, newContent))
  }

  it should "find scalar using path expression key" in {
    val f = StringFileArtifact("test.yml", YmlUsageTestTargets.xYml)
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

  it should "parse and run path expression using name" in {
    val f = StringFileArtifact("test.yml", YmlUsageTestTargets.xYml)
    val tn = typeBeingTested.fileToRawNode(f).get
    //println(TreeNodeUtils.toShorterString(tn, TreeNodeUtils.NameAndContentStringifier))

    val nodes = evaluatePathExpression(tn, "/dependencies")
    assert(nodes.size == 1)
    val nodes2 = evaluatePathExpression(tn, "/dependencies/*")
    assert(nodes2.size === 12)
    assert(NodeUtils.value(nodes2.last) === "God Save the Queen")
  }

  it should "parse and run path expression using type" in {
    val f = StringFileArtifact("test.yml", YmlUsageTestTargets.xYml)
    val tn = typeBeingTested.fileToRawNode(f).get
    //println(TreeNodeUtils.toShorterString(tn, TreeNodeUtils.NameAndContentStringifier))

    val nodes2 = evaluatePathExpression(tn, "/Sequence()[@name='dependencies']/*")
    assert(nodes2.size === 12)
    assert(NodeUtils.value(nodes2.last) === "God Save the Queen")
  }

  it should "parse and run path expression against YamlOrgStart invoice" in pendingUntilFixed {
    val f = StringFileArtifact("test.yml", YmlUsageTestTargets.YamlOrgStart)
    val tn = typeBeingTested.fileToRawNode(f).get
    //println(TreeNodeUtils.toShorterString(tn, TreeNodeUtils.NameAndContentStringifier))

    val nodes = evaluatePathExpression(tn, "//bill-to/given")
    assert(nodes.size == 1)
  }

}
