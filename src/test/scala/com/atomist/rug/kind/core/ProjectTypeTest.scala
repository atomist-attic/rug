package com.atomist.rug.kind.core

import com.atomist.parse.java.ParsingTargets
import com.atomist.rug.kind.DefaultTypeRegistry
import com.atomist.rug.runtime.js.{SimpleContainerGraphNode, SimpleExecutionContext}
import com.atomist.tree.SimpleTerminalTreeNode
import com.atomist.tree.pathexpression.{PathExpression, PathExpressionEngine}
import org.scalatest.{FlatSpec, Matchers}

class ProjectTypeTest extends FlatSpec with Matchers {

  import com.atomist.tree.pathexpression.PathExpressionParser._

  val pe = new PathExpressionEngine

  "project type" should "resolve Project under Repo using branch" in
    testBranch("thing")

  it should "resolve Project under Repo using branch with special characters in name" in
    Seq("the-thing", "the$thing", "thing451", "387", "#387").foreach(testBranch)

  private def testBranch(branch: String) {
    val owner = "atomist"
    val repo = "rug"
    val as = ParsingTargets.NewStartSpringIoProject
    val ec = SimpleExecutionContext(DefaultTypeRegistry,
      Some(FixedBranchRepoResolver(owner, repo, branch, as)))
    val node = SimpleContainerGraphNode.empty("Repo", "Repo")
      .addRelatedNode(SimpleTerminalTreeNode("owner", owner))
      .addRelatedNode(SimpleTerminalTreeNode("name", repo))
    val pex: PathExpression = s"/Repo()/$branch::Project()"
    pe.evaluate(SimpleContainerGraphNode("root", node), pex, ec) match {
      case Right(nodes) =>
        nodes.size should be(1)
        val proj = nodes.head.asInstanceOf[ProjectMutableView]
        assert(proj.totalFileCount === as.totalFileCount)
      case x => fail(s"Unexpected: $x")
    }
  }

  it should "resolve Project under Repo using sha" in {
    val sha = "d6cd1e2bd19e03a81132a23b2025920577f84e37"
    val owner = "atomist"
    val repo = "rug"
    val as = ParsingTargets.NewStartSpringIoProject
    val ec = SimpleExecutionContext(DefaultTypeRegistry,
      Some(FixedShaRepoResolver(owner, repo, sha, as)))
    val node = SimpleContainerGraphNode.empty("Repo", "Repo")
      .addRelatedNode(SimpleTerminalTreeNode("owner", owner))
      .addRelatedNode(SimpleTerminalTreeNode("name", repo))
    val pex: PathExpression = s"/Repo()/$sha::Project()"
    pe.evaluate(SimpleContainerGraphNode("root", node), pex, ec) match {
      case Right(nodes) =>
        nodes.size should be(1)
        val proj = nodes.head.asInstanceOf[ProjectMutableView]
        assert(proj.totalFileCount === as.totalFileCount)
      case x => fail(s"Unexpected: $x")
    }
  }

  it should "resolve Project under Commit" in {
    val sha = "d6cd1e2bd19e03a81132a23b2025920577f84e37"
    val owner = "atomist"
    val repo = "rug"
    val as = ParsingTargets.NewStartSpringIoProject
    val ec = SimpleExecutionContext(DefaultTypeRegistry,
      Some(FixedShaRepoResolver(owner, repo, sha, as)))
    val repoNode = SimpleContainerGraphNode.empty("Repo", "Repo")
      .addRelatedNode(SimpleTerminalTreeNode("owner", owner))
      .addRelatedNode(SimpleTerminalTreeNode("name", repo))
    val commitNode = SimpleContainerGraphNode.empty("Commit", "Commit")
      .addRelatedNode(SimpleTerminalTreeNode("sha", sha))
      .addRelatedNode(repoNode)
    val pex: PathExpression = s"/Commit()/source::Project()"
    pe.evaluate(SimpleContainerGraphNode("root", commitNode), pex, ec) match {
      case Right(nodes) =>
        nodes.size should be(1)
        val proj = nodes.head.asInstanceOf[ProjectMutableView]
        assert(proj.totalFileCount === as.totalFileCount)
      case x => fail(s"Unexpected: $x")
    }
  }

  it should "resolve Project after Commit" in
    navigatePush("after")

  it should "resolve Project before Commit" in
    navigatePush("before")

  private def navigatePush(navigation: String) {
    val sha = "d6cd1e2bd19e03a81132a23b2025920577f84e37"
    val owner = "atomist"
    val repo = "rug"
    val as = ParsingTargets.NewStartSpringIoProject
    val ec = SimpleExecutionContext(DefaultTypeRegistry,
      Some(FixedShaRepoResolver(owner, repo, sha, as)))
    val repoNode = SimpleContainerGraphNode.empty("Repo", "Repo")
      .addRelatedNode(SimpleTerminalTreeNode("owner", owner))
      .addRelatedNode(SimpleTerminalTreeNode("name", repo))
    val commitNode = SimpleContainerGraphNode.empty("Commit", "Commit")
      .addRelatedNode(SimpleTerminalTreeNode("sha", sha))
      .addRelatedNode(repoNode)
    val pushNode = SimpleContainerGraphNode.empty("Push", "Push")
      .addRelatedNode(SimpleTerminalTreeNode("sha", sha))
      .addRelatedNode(repoNode)
      .addEdge(navigation, Seq(commitNode))
    val pex: PathExpression = s"/Push()/$navigation::Commit()/source::Project()"
    pe.evaluate(SimpleContainerGraphNode("root", pushNode), pex, ec) match {
      case Right(nodes) =>
        nodes.size should be(1)
        val proj = nodes.head.asInstanceOf[ProjectMutableView]
        assert(proj.totalFileCount === as.totalFileCount)
      case x => fail(s"Unexpected: $x")
    }
  }

  it should "reject invalid Push navigation" in {
    val sha = "d6cd1e2bd19e03a81132a23b2025920577f84e37"
    val owner = "atomist"
    val repo = "rug"
    val as = ParsingTargets.NewStartSpringIoProject
    val ec = SimpleExecutionContext(DefaultTypeRegistry,
      Some(FixedShaRepoResolver(owner, repo, sha, as)))
    val repoNode = SimpleContainerGraphNode.empty("Repo", "Repo")
      .addRelatedNode(SimpleTerminalTreeNode("owner", owner))
      .addRelatedNode(SimpleTerminalTreeNode("name", repo))
    val commitNode = SimpleContainerGraphNode.empty("Commit", "Commit")
      .addRelatedNode(SimpleTerminalTreeNode("sha", sha))
      .addRelatedNode(repoNode)
    val pushNode = SimpleContainerGraphNode.empty("Push", "Push")
      .addRelatedNode(SimpleTerminalTreeNode("sha", sha))
      .addRelatedNode(repoNode)
      .addEdge("after", Seq(commitNode))
    val pex: PathExpression = s"/Push()/nonsense::Commit/source::Project()"
    an[RuntimeException] shouldBe thrownBy(
      pe.evaluate(SimpleContainerGraphNode("root", pushNode), pex, ec)
    )
  }
}
