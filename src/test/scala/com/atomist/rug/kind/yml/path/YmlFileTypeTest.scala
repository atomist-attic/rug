package com.atomist.rug.kind.yml.path

import com.atomist.rug.kind.grammar.{AbstractTypeUnderFileTest, TypeUnderFile}
import com.atomist.rug.kind.yml.YmlUsageTest
import com.atomist.source.StringFileArtifact
import com.atomist.tree.utils.TreeNodeUtils

class YmlFileTypeTest extends AbstractTypeUnderFileTest {

  override protected def typeBeingTested: TypeUnderFile = new YmlFileType

  it should "parse and output unchanged" in {
    val f = StringFileArtifact("test.yml", YmlUsageTest.xYml)
    val tn = typeBeingTested.fileToRawNode(f).get
    println(TreeNodeUtils.toShorterString(tn, TreeNodeUtils.NameAndContentStringifier))

    withClue(s"Was [${tn.value}]\nExpected [${f.content}]") {
      assert(tn.value === f.content)
    }
  }

  it should "parse and run path expression using name" in {
    val f = StringFileArtifact("test.yml", YmlUsageTest.xYml)
    val tn = typeBeingTested.fileToRawNode(f).get
    println(TreeNodeUtils.toShorterString(tn, TreeNodeUtils.NameAndContentStringifier))

    val nodes = evaluatePathExpression(tn, "/dependencies")
    assert(nodes.size == 1)
    val nodes2 = evaluatePathExpression(tn, "/dependencies/*")
    assert(nodes2.size === 12)
    assert(nodes2.last.value === "God Save the Queen")
  }

  it should "parse and run path expression using type" in {
    val f = StringFileArtifact("test.yml", YmlUsageTest.xYml)
    val tn = typeBeingTested.fileToRawNode(f).get
    println(TreeNodeUtils.toShorterString(tn, TreeNodeUtils.NameAndContentStringifier))

    val nodes2 = evaluatePathExpression(tn, "/Sequence()[@name='dependencies']/*")
    assert(nodes2.size === 12)
    assert(nodes2.last.value === "God Save the Queen")
  }
}
