package com.atomist.rug.exec

import com.atomist.project.{Executor, SimpleProjectOperationArguments}
import com.atomist.rug.compiler.typescript.TypeScriptCompiler
import com.atomist.rug.kind.service._
import com.atomist.rug.runtime.js.interop.jsPathExpressionEngine
import com.atomist.rug.runtime.rugdsl.RugDrivenExecutor
import com.atomist.rug.ts.RugTranspiler
import com.atomist.rug.{CompilerChainPipeline, DefaultRugPipeline, RugPipeline, TestUtils}
import com.atomist.source._
import com.typesafe.scalalogging.LazyLogging
import org.scalatest.{FlatSpec, Matchers}

import scala.collection.mutable.ListBuffer

class RugDrivenExecutorTest extends FlatSpec with Matchers {

  val ccPipeline: RugPipeline = new CompilerChainPipeline(Seq(new RugTranspiler(), new TypeScriptCompiler()))

  it should "update all projects" in {
    val content = "What is this, the high hat?"
    updateAllProjects(content,
      s"""
         |executor AddSomeCaspar
         |
         |with Services s
         | editWith Caspar
         |
         |editor Caspar
         |with Project p
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
         |with Services
         | editWith Caspar
         | onNoChange do raiseIssue "foobar"
         |
         |editor Caspar
         |with Project p when { !p.name().equals("caspared") }
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
         |with Services s
         | editWith Caspar
         | onNoChange do eval { s.raiseIssue("foobar") }
         |
         |editor Caspar
         |with Project p when { !p.name().equals("caspared") }
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
         |with Project p when { !p.name().equals("caspared") }
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

    latestVersions.size should be>=2
    latestVersions.foreach(source => source.findFile("Caspar").get.content.equals(content) should be(true))
    // We should have raised the issue for the one service that already had this file
    services.issues.size should be (1)
  }

  it should "update all projects using JavaScript editor action" in {
    val content = "What is this, the high hat?"
    updateAllProjects(content,
      s"""
         |executor AddSomeCaspar
         |
         |with Services s
         | editWith Caspar
         |
         |editor Caspar
         |with Project
         |  do addFile "Caspar"  "$content"
      """.stripMargin
    )
  }

  it should "update all projects using JavaScript executor action" in {
    val content = "What is this, the high hat?"
    updateAllProjects(content,
      s"""
         |import {Executor} from "@atomist/rug/operations/Executor"
         |import {Services} from "@atomist/rug/model/Core"
         |import {Result,Status} from "@atomist/rug/operations/RugOperation"
         |
         |class AddSomeCaspar implements Executor {
         |    description: string = "An angry executor"
         |    name: string = "AddSomeCaspar"
         |    execute(services: Services): Result {
         |        for (let s of services.services())
         |            s.addFile("Caspar", "$content");
         |
         |      return new Result(Status.Success, "OK")
         |    }
         |}
         |
         |var caspar = new AddSomeCaspar()
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
    val as = new SimpleFileBasedArtifactSource("", StringFileArtifact(rugPath, rug)) + TestUtils.user_model

    //val files = as.allFiles
    val ops = pipeline.create(as,None)
    val CasparRangesFree = ops.head.asInstanceOf[Executor]

    val donny = EmptyArtifactSource("")
    val dude = new SimpleFileBasedArtifactSource("dude", StringFileArtifact("question", "Mind if i light up a J?"))
    val services = new FakeServiceSource(Seq(donny, dude))
    CasparRangesFree.execute(services, SimpleProjectOperationArguments.Empty)

    val latestVersions = services.updatePersister.latestVersion.values
    latestVersions.size should be(2)
    latestVersions.foreach(source => source.findFile("Caspar").get.content.equals(content) should be(true))
  }
}

class FakeServiceSource(val projects: Seq[ArtifactSource]) extends ServiceSource with IssueRouter {

  val updatePersister = new FakeUpdatePersister

  val teamId = "atomist-test"

  override def pathExpressionEngine: jsPathExpressionEngine =
    new jsPathExpressionEngine(teamContext = this)

  override def messageBuilder: MessageBuilder =
    new ConsoleMessageBuilder(teamId, EmptyActionRegistry)

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