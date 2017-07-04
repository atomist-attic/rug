package com.atomist.tree.marshal

import com.atomist.rug.TestUtils
import com.atomist.rug.kind.DefaultTypeRegistry
import com.atomist.rug.ts.Cardinality
import com.atomist.tree.TreeNode
import com.atomist.tree.utils.NodeUtils
import org.scalatest.{FlatSpec, Matchers}

class LinkedJsonGraphDeserializerTest extends FlatSpec with Matchers {

  "json graph deserializer" should "deserialize simple tree" in {
    val node = LinkedJsonGraphDeserializer.fromJson(TestUtils.contentOf(this, "simple.json"))
    assert(node.nodeTags === Set("Issue", TreeNode.Dynamic))
    assert(node.relatedNodesNamed("number").size === 1)
  }

  it should "deserialize a tree of n depth" in {
    val withLinks = TestUtils.contentOf(this, "withLinks.json")
    val node = LinkedJsonGraphDeserializer.fromJson(withLinks)
    assert(node.nodeTags === Set("Build", TreeNode.Dynamic))
    assert(node.relatedNodesNamed("status").head.asInstanceOf[TreeNode].value === "Passed")
    val repo = node.relatedNodesNamed("ON").head
    assert(repo.relatedNodesNamed("owner").size === 1)
    val chatChannel = repo.relatedNodesNamed("CHANNEL").head
    assert(chatChannel.relatedNodesNamed("name").size === 1)
    assert(chatChannel.relatedNodesNamed("id").head.asInstanceOf[TreeNode].value === "channel-id")
  }

  it should "distinguish cardinality" in {
    val withLinks = TestUtils.contentOf(this, "withLinks.json")
    val node = LinkedJsonGraphDeserializer.fromJson(withLinks)
    assert(node.nodeTags === Set("Build", TreeNode.Dynamic))
    assert(node.relatedNodesNamed("status").head.asInstanceOf[TreeNode].value === "Passed")
    val repo = node.relatedNodesNamed("ON").head
    assert(repo.relatedNodesNamed("owner").size === 1)
    assert(!repo.nodeTags.contains(Cardinality.One2Many))
    assert(!node.nodeTags.contains(Cardinality.One2Many))

    // Special node with cardinality
    val contains = node.relatedNodesNamed("ONM").head
    assert(contains.nodeTags.contains(Cardinality.One2Many))

    val chatChannel = repo.relatedNodesNamed("CHANNEL").head
    assert(chatChannel.relatedNodesNamed("name").size === 1)
    assert(chatChannel.relatedNodesNamed("id").head.asInstanceOf[TreeNode].value === "channel-id")
  }

  it should "handle an empty result set" in {
    val node = LinkedJsonGraphDeserializer.fromJson("[]")
    assert(node.relatedNodeNames.isEmpty)
    assert(node.nodeTags.isEmpty)
    assert(node.relatedNodeTypes.isEmpty)
    assert(node.nodeName === "empty")
  }

  it should "handle unresolvable in Repo -> Project" in {
    val json = TestUtils.contentOf(this, "withLinksAndUnresolvable.json")
    val node = LinkedJsonGraphDeserializer.fromJson(json)
    val repo = node.relatedNodesNamed("ON").head
    assert(repo.nodeTags.contains("Repo"))
    assert(NodeUtils.keyValue(repo, "name").contains("repo-name"))
    assert(repo.relatedNodesNamed("PROJECT").size === 1)
    repo.relatedNodesNamed("PROJECT").head match {
      case ur if Unresolvable(ur).isDefined =>
        assert(ur.nodeTags.contains("Unresolvable"))
      case x => fail(s"Expected Unresolvable node, not $x")
    }
  }

  it should "handle unresolvable impact events" in {
    val json = TestUtils.contentOf(this, "withLinksAndUnresolvableImpact.json")
    val node = LinkedJsonGraphDeserializer.fromJson(json)
    val repo = node.relatedNodesNamed("ON").head
    assert(repo.relatedNodesNamed("IMPACT").size === 1)
    repo.relatedNodesNamed("IMPACT").head match {
      case ur if Unresolvable(ur).isDefined =>
        assert(ur.nodeTags.contains("Unresolvable"))
      case x => fail(s"Expected Unresolvable node, not $x")
    }
  }

  it should "deserialize graph with cycles and ensure toString is safe" in {
    val withLinks = TestUtils.contentOf(this, "herokuGraph.json")
    val node = LinkedJsonGraphDeserializer.fromJson(withLinks)
    val x = node.toString
    assert(x.contains("HerokuApp"))
  }
}
