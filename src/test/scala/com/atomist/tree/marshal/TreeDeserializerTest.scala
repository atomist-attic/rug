package com.atomist.tree.marshal

import org.scalatest.{FlatSpec, Matchers}

class TreeDeserializerTest extends FlatSpec with Matchers {

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
    val node = TreeDeserializer.fromJson(t1)
    node.nodeType should be ("Issue")
    val k = node.childrenNamed("number").size should be (1)
  }
}
