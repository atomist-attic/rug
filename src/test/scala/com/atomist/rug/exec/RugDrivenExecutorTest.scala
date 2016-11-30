package com.atomist.rug.exec

import com.atomist.rug.DefaultRugPipeline
import com.atomist.rug.kind.service._
import com.atomist.rug.runtime.RugDrivenExecutor
import com.atomist.source._
import com.typesafe.scalalogging.LazyLogging
import org.scalatest.{FlatSpec, Matchers}

import scala.collection.mutable.ListBuffer

class RugDrivenExecutorTest extends FlatSpec with Matchers {

  it should "update all projects" in {
    val content = "What is this, the high hat?"
    updateAllProjects(content,
      s"""
         |executor AddSomeCaspar
         |
         |#let x = from services s with file f return name
         |
         |with services s
         | editWith Caspar
         |
         |editor Caspar
         |with project p
         | do addFile "Caspar" "$content"
      """.stripMargin
    )
  }

  it should "update all projects but message about ones that don't change" in {
    val content = "What is this, the high hat?"
    val rug =
      s"""
         |executor AddSomeCaspar
         |
         |with services
         | editWith Caspar
         | onNoChange do raiseIssue "foobar"
         |
         |editor Caspar
         |with project p when { !p.name().equals("caspared") }
         | do addFile "Caspar" "$content"
      """.stripMargin
    updateAllProjectsButRaiseIssueForOnesThatDontChange(content, rug)
  }

  it should "allow JavaScript in noChange block" in {
    val content = "What is this, the high hat?"
    val rug =
      s"""
         |executor AddSomeCaspar
         |
         |with services s
         | editWith Caspar
         | onNoChange do eval { s.raiseIssue("foobar") }
         |
         |editor Caspar
         |with project p when { !p.name().equals("caspared") }
         | do addFile "Caspar" "$content"
      """.stripMargin
    updateAllProjectsButRaiseIssueForOnesThatDontChange(content, rug)
  }

  // TODO need to decide on exact model for this
  it should "allow compound noChange block" in pendingUntilFixed {
    val content = "What is this, the high hat?"
    val rug =
      s"""
         |executor AddSomeCaspar
         |
         |with services
         | editWith Caspar
         | onNoChange begin
         |    raiseIssue "foo"
         | end
         |
         |editor Caspar
         |with project p when { !p.name().equals("caspared") }
         | do addFile "Caspar" "$content"
      """.stripMargin
    updateAllProjectsButRaiseIssueForOnesThatDontChange(content, rug)
  }

  private def updateAllProjectsButRaiseIssueForOnesThatDontChange(content: String, rug: String) = {
    val rp = new DefaultRugPipeline()
    val CasparRangesFree = rp.create(
      new SimpleFileBasedArtifactSource("", StringFileArtifact("executors/AddSomeCaspar.rug", rug)),
      None
    ).head.asInstanceOf[RugDrivenExecutor]

    val donny = EmptyArtifactSource("")
    val dude = new SimpleFileBasedArtifactSource("dude", StringFileArtifact("question", "Mind if i light up a J?"))
    val alreadyHasCaspar = new SimpleFileBasedArtifactSource("caspared", StringFileArtifact("Caspar", content))

    val services = new FakeServiceSource(Seq(donny, dude, alreadyHasCaspar))
    CasparRangesFree.execute(services)

    val latestVersions = services.updatePersister.latestVersion.values

    latestVersions.size should be>=(2)
    latestVersions.foreach(_.findFile("Caspar").get.content.equals(content) should be(true))
    // We should have raised the issue for the one service that already had this file
    services.issues.size should be (1)
  }

  it should "update all projects using JavaScript editor action" in {
    val content = "What is this, the high hat?"
    updateAllProjects(content,
      s"""
         |executor AddSomeCaspar
         |
         |with services s
         | editWith Caspar
         |
         |editor Caspar
         |{
         | project.addFile("Caspar" , "$content");
         |}
      """.stripMargin
    )
  }

  // TODO convert to TypeScript
//  it should "update all projects using JavaScript executor action" in {
//    val content = "What is this, the high hat?"
//    updateAllProjects(content,
//      s"""
//         |executor AddSomeCaspar
//         |
//         |{
//         |  load("http://underscorejs.org/underscore.js");
//         |  _.each(services.services(),
//         |      function(project){ project.addFile("Caspar" , "$content") });
//         |}
//      """.stripMargin
//    )
  }

  it should "update all projects using JavaScript executor action invoking named editor" in pendingUntilFixed {
    val content = "What is this, the high hat?"
    updateAllProjects(content,
      s"""
         |executor AddSomeCaspar
         |
         |{
         |  load("http://underscorejs.org/underscore.js");
         |  _.each(services.services(),
         |      function(project){ project.editWith("Caspar") });
         |}
         |
         |editor Caspar
         |{
         |  project.addFile("Caspar" , "$content");
         |}
      """.stripMargin
    )
  }

  private def updateAllProjects(content: String, rug: String) {
    val rp = new DefaultRugPipeline()
    val CasparRangesFree = rp.create(
      new SimpleFileBasedArtifactSource("", StringFileArtifact("executors/AddSomeCaspar.rug", rug)),
      None
    ).head.asInstanceOf[RugDrivenExecutor]

    val donny = EmptyArtifactSource("")
    val dude = new SimpleFileBasedArtifactSource("dude", StringFileArtifact("question", "Mind if i light up a J?"))
    val services = new FakeServiceSource(Seq(donny, dude))
    CasparRangesFree.execute(services)

    val latestVersions = services.updatePersister.latestVersion.values
    latestVersions.size should be(2)
    latestVersions.foreach(_.findFile("Caspar").get.content.equals(content) should be(true))
  }
}

class FakeServiceSource(val projects: Seq[ArtifactSource]) extends ServiceSource with IssueRouter {

  val updatePersister = new FakeUpdatePersister

  override def userMessageRouter: UserMessageRouter = ConsoleUserMessageRouter

  var issues = ListBuffer.empty[Issue]

  override def services: Seq[Service] =
    projects.map(proj => Service(proj, updatePersister, issueRouter = this))

  override def raiseIssue(service: Service, issue: Issue): Unit = issues.append(issue)
}

class FakeUpdatePersister extends UpdatePersister with LazyLogging {

  var latestVersion: Map[ArtifactSourceIdentifier, ArtifactSource] = Map()

  override def update(service: Service, newContent: ArtifactSource, updateIdentifier: String): Unit = {
    logger.debug(s"Service $service updated")
    latestVersion += (service.project.id -> newContent)
  }
}