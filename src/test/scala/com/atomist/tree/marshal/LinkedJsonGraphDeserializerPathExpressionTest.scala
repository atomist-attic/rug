package com.atomist.tree.marshal

import com.atomist.parse.java.ParsingTargets
import com.atomist.rug.TestUtils
import com.atomist.rug.kind.DefaultTypeRegistry
import com.atomist.rug.kind.core.{ProjectMutableView, RepoResolver}
import com.atomist.rug.runtime.js.{SimpleContainerGraphNode, SimpleExecutionContext}
import com.atomist.source.ArtifactSource
import com.atomist.tree.TreeNode
import com.atomist.tree.pathexpression.{PathExpression, PathExpressionEngine}
import org.scalatest.{FlatSpec, Matchers}

class LinkedJsonGraphDeserializerPathExpressionTest extends FlatSpec with Matchers {

  import com.atomist.tree.pathexpression.PathExpressionParser.parseString

  val pe = new PathExpressionEngine

  "deserialized JSON: path expressions" should "work against simple tree" in {
    val node = LinkedJsonGraphDeserializer.fromJson(TestUtils.contentOf(this, "simple.json"))
    pe.evaluate(SimpleContainerGraphNode("root", node), "/Issue()") match {
      case Right(nodes) =>
        assert(nodes.size === 1)
      case x => fail(s"Unexpected: $x")
    }
  }

  it should "work against tree of n depth" in {
    val withLinks = TestUtils.contentOf(this, "withLinks.json")
    val node = LinkedJsonGraphDeserializer.fromJson(withLinks)
    pe.evaluate(SimpleContainerGraphNode("root", node),
      "/Build()[@status='Passed']/ON::Repo()/CHANNEL::ChatChannel()") match {
      case Right(nodes) =>
        assert(nodes.size === 1)
        println(nodes.head)
      case x => fail(s"Unexpected: $x")
    }

    assert(node.nodeTags === Set("Build", TreeNode.Dynamic))
    assert(node.relatedNodesNamed("status").head.asInstanceOf[TreeNode].value === "Passed")
    val repo = node.relatedNodesNamed("ON").head
    assert(repo.relatedNodesNamed("owner").size === 1)
    val chatChannel = repo.relatedNodesNamed("CHANNEL").head
    assert(chatChannel.relatedNodesNamed("name").size === 1)
    assert(chatChannel.relatedNodesNamed("id").head.asInstanceOf[TreeNode].value === "channel-id")
  }

  it should "handle an empty result set" in {
    val node = LinkedJsonGraphDeserializer.fromJson("[]")
    pe.evaluate(SimpleContainerGraphNode("root", node), "/*") match {
      case Right(nodes) =>
        nodes.size should be (1)
        assert(nodes.head.nodeTags === Set.empty)
      case x => fail(s"Unexpected: $x")
    }
  }

  it should "handle unresolvable in Repo -> Project using master" in {
    val as = ParsingTargets.NewStartSpringIoProject
    val ec = SimpleExecutionContext(DefaultTypeRegistry, Some(FixedBranchRepoResolver("master", as)))
    val json = TestUtils.contentOf(this, "withLinksAndUnresolvable.json")
    val node = LinkedJsonGraphDeserializer.fromJson(json)
    val pex: PathExpression = "/Build()/ON::Repo()/master::Project()"
    pe.evaluate(SimpleContainerGraphNode("root", node), pex, ec) match {
      case Right(nodes) =>
        nodes.size should be (1)
        val proj = nodes.head.asInstanceOf[ProjectMutableView]
        assert(proj.totalFileCount === as.totalFileCount)
      case x => fail(s"Unexpected: $x")
    }
  }

  it should "handle unresolvable in Repo -> Project using sha" in {
    val sha = "d6cd1e2bd19e03a81132a23b2025920577f84e37"
    val as = ParsingTargets.NewStartSpringIoProject
    val ec = SimpleExecutionContext(DefaultTypeRegistry, Some(FixedShaRepoResolver(sha, as)))
    val json = TestUtils.contentOf(this, "withLinksAndUnresolvable.json")
    val node = LinkedJsonGraphDeserializer.fromJson(json)
    val pex: PathExpression = s"/Build()/ON::Repo()/$sha::Project()"
    pe.evaluate(SimpleContainerGraphNode("root", node), pex, ec) match {
      case Right(nodes) =>
        nodes.size should be (1)
        val proj = nodes.head.asInstanceOf[ProjectMutableView]
        assert(proj.totalFileCount === as.totalFileCount)
      case x => fail(s"Unexpected: $x")
    }

  }
}

case class FixedBranchRepoResolver(expectedBranch: String, as: ArtifactSource) extends RepoResolver {

  override def resolveBranch(owner: String, repoName: String, branch: String): ArtifactSource =
    branch match {
      case `expectedBranch` => as
      case x => throw new IllegalArgumentException(s"Don't know about branch [$branch]")
    }

  override def resolveSha(owner: String, repoName: String, sha: String): ArtifactSource = ???
}

case class FixedShaRepoResolver(expectedSha: String, as: ArtifactSource) extends RepoResolver {

  override def resolveBranch(owner: String, repoName: String, branch: String): ArtifactSource = ???

  override def resolveSha(owner: String, repoName: String, sha: String): ArtifactSource =
    sha match {
      case `expectedSha` => as
      case x => throw new IllegalArgumentException(s"Don't know about sha [$sha]")
    }
}
