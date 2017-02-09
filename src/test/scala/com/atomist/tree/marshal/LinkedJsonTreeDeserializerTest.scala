package com.atomist.tree.marshal

import com.atomist.tree.TreeNode
import org.scalatest.{FlatSpec, Matchers}

class LinkedJsonTreeDeserializerTest extends FlatSpec with Matchers {

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

  val t2: String =
    """
     |[
     |   {
     |      "build_url":"https://travis-ci.org/something",
     |      "id":"192756197",
     |      "timestamp":"2017-01-17T17:21:48.374Z",
     |      "started_at":"2017-01-17T17:18:57Z",
     |      "nodeId":4770,
     |      "name":"333",
     |      "compare_url":"https://github.com/something",
     |      "status":"Passed",
     |      "type":[
     |         "Build"
     |      ],
     |      "finished_at":"2017-01-17T17:21:45Z"
     |   },
     |   {
     |      "message":"Commit message",
     |      "sha":"sha",
     |      "timestamp":"2017-01-17T11:18:51-06:00",
     |      "nodeId":4767,
     |      "type":[
     |         "Commit"
     |      ]
     |   },
     |   {
     |      "name":"user-name",
     |      "login":"user-login",
     |      "email":"user-email",
     |      "nodeId":2900,
     |      "type":[
     |         "GitHubId"
     |      ]
     |   },
     |   {
     |      "owner":"owner",
     |      "name":"repo-name",
     |      "nodeId":2391,
     |      "type":[
     |         "Repo"
     |      ]
     |   },
     |   {
     |      "name":"channel-name",
     |      "id":"channel-id",
     |      "nodeId":2200,
     |      "type":[
     |         "ChatChannel",
     |         "SlackChannel"
     |      ]
     |   },
     |   {
     |      "before":"before-sha",
     |      "after":"after-sha",
     |      "branch":"git-branch",
     |      "timestamp":"2017-01-17T17:18:52.943Z",
     |      "nodeId":4766,
     |      "type":[
     |         "Push"
     |      ]
     |   },
     |   {
     |      "startNodeId":4767,
     |      "endNodeId":4770,
     |      "type":"HAS_BUILD"
     |   },
     |   {
     |      "startNodeId":4767,
     |      "endNodeId":2900,
     |      "type":"AUTHOR"
     |   },
     |   {
     |      "startNodeId":4770,
     |      "endNodeId":2391,
     |      "type":"ON"
     |   },
     |   {
     |      "startNodeId":2391,
     |      "endNodeId":2200,
     |      "type":"CHANNEL"
     |   },
     |   {
     |      "startNodeId":4770,
     |      "endNodeId":4766,
     |      "type":"TRIGGERED_BY"
     |   },
     |   {
     |      "startNodeId":4766,
     |      "endNodeId":4767,
     |      "type":"CONTAINS"
     |   },
     |   {
     |      "startNodeId":4767,
     |      "endNodeId":2900,
     |      "type":"AUTHOR"
     |   }
     |]
    """.stripMargin

  it should "deserialize simple tree" in {
    val node = LinkedJsonTreeDeserializer.fromJson(t1)
    assert(node.nodeTags === Set("Issue", TreeNode.Dynamic))
    assert(node.childrenNamed("number").size === 1)
  }

  it should "deserialize a tree of n depth" in {
    val node = LinkedJsonTreeDeserializer.fromJson(t2)
    assert(node.nodeTags === Set("Build", TreeNode.Dynamic))
    assert(node.childrenNamed("status").head.value === "Passed")
    val repo = node.childrenNamed("ON").head
    assert(repo.childrenNamed("owner").size === 1)
    val chatChannel = repo.childrenNamed("CHANNEL").head
    assert(chatChannel.childrenNamed("name").size === 1)
    assert(chatChannel.childrenNamed("id").head.value === "channel-id")
  }
}
