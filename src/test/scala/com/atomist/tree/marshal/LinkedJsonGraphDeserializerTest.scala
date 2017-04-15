package com.atomist.tree.marshal

import com.atomist.rug.TestUtils
import com.atomist.rug.ts.Cardinality
import com.atomist.tree.TreeNode
import org.scalatest.{FlatSpec, Matchers}

class LinkedJsonGraphDeserializerTest extends FlatSpec with Matchers {

  val t1: String =
    """
      |[
      |  {
      |    "number": 7,
      |    "state": "closed",
      |    "id": "189105883",
      |    "title": "and something more",
      |    "body": "",
      |    "nodeId": 2179,
      |    "type": [
      |      "Issue"
      |    ]
      |  },
      |  {
      |    "message": "crushing issue #7",
      |    "sha": "c239591ccd28dea8ce7ee5728df779f063f3f677",
      |    "nodeId": 2184,
      |    "type": [
      |      "Commit"
      |    ]
      |  },
      |  {
      |    "login": "Jim Clark",
      |    "email": "slimslenderslacks@gmail.com",
      |    "nodeId": 1590,
      |    "type": [
      |      "GitHubId"
      |    ]
      |  },
      |  {
      |    "startNodeId": 2179,
      |    "endNodeId": 2184,
      |    "type": "RESOLVED_BY"
      |  },
      |  {
      |    "startNodeId": 2184,
      |    "endNodeId": 1590,
      |    "type": "AUTHOR"
      |  }
      |]
    """.stripMargin

  it should "deserialize simple tree" in {
    val node = LinkedJsonGraphDeserializer.fromJson(t1)
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
    assert(repo.relatedNodesNamed("PROJECT").size === 1)
    repo.relatedNodesNamed("PROJECT").head match {
      case ur if Unresolvable(ur).isDefined =>
        assert(ur.nodeTags.contains("Unresolvable"))
      case x => fail(s"Expected Unresolvable node, not $x")
    }

  }
}
