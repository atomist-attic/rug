package com.atomist.rug.exec

import com.atomist.project.SimpleProjectOperationArguments
import com.atomist.project.archive.ProjectOperationArchiveReader
import com.atomist.rug.compiler.typescript.TypeScriptCompiler
import com.atomist.rug.kind.service._
import com.atomist.rug.runtime.rugdsl.RugDrivenExecutor
import com.atomist.rug.{CompilerChainPipeline, DefaultRugPipeline, RugPipeline}
import com.atomist.source._
import com.typesafe.scalalogging.LazyLogging
import org.scalatest.{FlatSpec, Matchers}

import scala.collection.mutable.ListBuffer

class RugDrivenExecutorTest extends FlatSpec with Matchers {

  val ccPipeline: RugPipeline = new CompilerChainPipeline(Seq(new TypeScriptCompiler()))

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

  it should "update all projects using JavaScript executor action" in {
    val content = "What is this, the high hat?"
    updateAllProjects(content,
      s"""
         |import {Executor} from "@atomist/rug/operations/Executor"
         |import {Parameters} from "@atomist/rug/operations/Parameters"
         |import {Services} from "@atomist/rug/model/Core"
         |import {Result,Status} from "@atomist/rug/operations/Result"
         |
         |import {executor} from "@atomist/rug/support/Metadata"
         |
         |@executor("An angry executor")
         |class AddSomeCaspar implements Executor<Parameters> {
         |
         |    execute(services: Services, p: Parameters): Result {
         |        for (let s of services.services())
         |            s.addFile("Caspar", "$content");
         |
         |      return new Result(Status.Success, "OK")
         |    }
         |}
      """.stripMargin,
      rugPath = ".atomist/executors/AddSomeCaspar.ts",
      ccPipeline
    )
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

  private def updateAllProjects(content: String, rug: String,
                                rugPath: String = "executors/AddSomeCaspar.rug",
                                pipeline: RugPipeline = new DefaultRugPipeline()) {
    val as = new SimpleFileBasedArtifactSource("", StringFileArtifact(rugPath, rug))
//    val CasparRangesFree = pipeline.create(
//      as,
//      None
//    ).head.asInstanceOf[RugDrivenExecutor]

    val ops = new ProjectOperationArchiveReader().findOperations(as, None, Nil)
    val CasparRangesFree = ops.executors.head

    val donny = EmptyArtifactSource("")
    val dude = new SimpleFileBasedArtifactSource("dude", StringFileArtifact("question", "Mind if i light up a J?"))
    val services = new FakeServiceSource(Seq(donny, dude))
    CasparRangesFree.execute(services, SimpleProjectOperationArguments.Empty)

    val latestVersions = services.updatePersister.latestVersion.values
    latestVersions.size should be(2)
    latestVersions.foreach(_.findFile("Caspar").get.content.equals(content) should be(true))
  }
}

class FakeServiceSource(val projects: Seq[ArtifactSource]) extends ServiceSource with IssueRouter {

  val updatePersister = new FakeUpdatePersister

  override def messageBuilder: MessageBuilder = new ConsoleMessageBuilder("TEAM_ID")

  var issues = ListBuffer.empty[Issue]

  override def services: Seq[Service] =
    projects.map(proj => Service(proj, updatePersister, issueRouter = this, messageBuilder = messageBuilder))

  override def raiseIssue(service: Service, issue: Issue): Unit = issues.append(issue)
}

class FakeUpdatePersister extends UpdatePersister with LazyLogging {

  var latestVersion: Map[ArtifactSourceIdentifier, ArtifactSource] = Map()

  override def update(service: Service, newContent: ArtifactSource, updateIdentifier: String): Unit = {
    logger.debug(s"Service $service updated")
    latestVersion += (service.project.id -> newContent)
  }
}