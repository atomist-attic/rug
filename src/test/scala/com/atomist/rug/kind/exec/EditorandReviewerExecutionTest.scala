package com.atomist.rug.kind.exec

import com.atomist.project.archive.{AtomistConfig, DefaultAtomistConfig}
import com.atomist.project.review.ReviewResult
import com.atomist.project.{Executor, SimpleProjectOperationArguments}
import com.atomist.rug.DefaultRugPipeline
import com.atomist.rug.kind.java.JavaClassTypeUsageTest
import com.atomist.rug.kind.service._
import com.atomist.source.{ArtifactSource, EmptyArtifactSource, SimpleFileBasedArtifactSource, StringFileArtifact}
import org.scalatest.{FlatSpec, Matchers}

import scala.collection.mutable.ListBuffer

class EditorAndReviewerExecutionTest extends FlatSpec with Matchers {

  val atomistConfig: AtomistConfig = DefaultAtomistConfig
  val emptyProject = EmptyArtifactSource("a")
  val littleProject = new SimpleFileBasedArtifactSource("b", StringFileArtifact("a", "b"))
  val bigProject = JavaClassTypeUsageTest.JavaAndText

  class DummyServiceSource(reviewOutput: Option[ReviewOutputPolicy] = None) extends ServiceSource {
    var latest: Map[Service, ArtifactSource] = Map()

    override def messageBuilder: MessageBuilder = new ConsoleMessageBuilder("TEAM_ID")

    override def services: Seq[Service] =
      Seq(emptyProject, littleProject, bigProject).map(as =>
        Service(as, new UpdatePersister {
          override def update(service: Service, newContent: ArtifactSource, updateIdentifier: String): Unit = latest += (service -> newContent)
        }, reviewOutput.getOrElse(IssueRaisingReviewOutputPolicy),
          messageBuilder = messageBuilder))
  }

  class BufferingReviewOutputPolicy extends ReviewOutputPolicy {
    val l: ListBuffer[ReviewResult] = new ListBuffer

    override def route(service: Service, rr: ReviewResult): Unit = {
      l.append(rr)
    }
  }

  it should "run a single editor against multiple projects" in {
    val executor =
      """
        |executor Foo
        |
        |with services s
        |   editWith Bar
        |
        |
        |editor Bar
        |
        |with project p
        |  do addFile "film.txt" "The Big Lebowski"
      """.stripMargin

    val arch = new SimpleFileBasedArtifactSource("",
      StringFileArtifact(atomistConfig.executorsRoot + "/Foo.rug", executor))
    val rp = new DefaultRugPipeline()
    val exs = rp.create(arch, None, Nil)
    exs.size should be(2)
    val ex: Executor = exs.collect {
      case e: Executor => e
    }.head
    val ls = new DummyServiceSource
    ex.execute(ls, SimpleProjectOperationArguments.Empty)
    ls.latest.size should be(3)
    ls.latest.values.foreach(as =>
      as.findFile("film.txt").get.content should equal("The Big Lebowski"))
  }

  it should "run a single editor against some projects" in {
    val executor =
      """
        |executor Foo
        |with services s when { s.name().length() > 0 }
        |   editWith Bar
        |
        |
        |editor Bar
        |with project p
        |  do addFile "film.txt" "The Big Lebowski"
      """.stripMargin

    val arch = new SimpleFileBasedArtifactSource("",
      StringFileArtifact(atomistConfig.executorsRoot + "/Foo.rug", executor))
    val rp = new DefaultRugPipeline()
    val exs = rp.create(arch, None, Nil)
    exs.size should be(2)
    val ex: Executor = exs.collect {
      case e: Executor => e
    }.head
    val ls = new DummyServiceSource
    ex.execute(ls, SimpleProjectOperationArguments.Empty)
    ls.latest.size should be(3)
    ls.latest.values.filter(_.totalFileCount > 2).foreach(as =>
      as.findFile("film.txt").get.content should equal("The Big Lebowski"))
  }

  it should "run a single reviewer against multiple projects" in
    executeReviewer(
      """
        |executor Foo
        |
        |with services s
        |   reviewWith Gripe
        |
        |reviewer Gripe
        |
        |with project p
        |  do majorProblem "I don't like Mondays"
      """.stripMargin, "Foo.rug", "Foo")

  it should "run a single reviewer against multiple projects 2" in
    executeReviewer(
      """
        |executor XMLChecker
        |with services s
        |  editWith Caspar
        |
        |editor Caspar
        |with project p
        |  do addFile "Caspar" "Put one in the brain"
        |
        |
        |executor GoCaspar
        |with services s
        |    reviewWith CasparAdvice
        |
        |reviewer CasparAdvice
        |with project p
        |  do majorProblem "What is this, the high hat?"
      """.stripMargin, "GoCaspar.rug", "GoCaspar")

  private def executeReviewer(executorProgram: String, filename: String, executorWeWant: String) {
    val arch = new SimpleFileBasedArtifactSource("",
      StringFileArtifact(atomistConfig.executorsRoot + "/" + filename, executorProgram))
    val rp = new DefaultRugPipeline()
    val exs = rp.create(arch, None, Nil)
    // exs.size should be (2)
    val ex: Executor = exs.collect {
      case e: Executor if e.name.equals(executorWeWant) => e
    }.head
    val ro = new BufferingReviewOutputPolicy
    val ls = new DummyServiceSource(Some(ro))
    ex.execute(ls, SimpleProjectOperationArguments.Empty)
    ls.latest.size should be(0) // Nothing got updated
    ro.l.size should be(ls.services.size)
  }

  it should "run a single editor against some projects with parameter" in {
    val executor =
      """
        |executor Foo
        |with services s when { s.name().length() > 0 }
        |   editWith Bar
        |
        |editor Bar
        |param film: .*
        |with project p
        |  do addFile "film.txt" film
      """.stripMargin

    val arch = new SimpleFileBasedArtifactSource("",
      StringFileArtifact(atomistConfig.executorsRoot + "/Foo.rug", executor))
    val rp = new DefaultRugPipeline()
    val exs = rp.create(arch, None, Nil)
    exs.size should be(2)
    val ex: Executor = exs.collect {
      case e: Executor => e
    }.head
    ex.parameters.map(p => p.getName).toSet should equal (Set("film"))
    val ls = new DummyServiceSource
    ex.execute(ls, SimpleProjectOperationArguments("", Map[String,String]("film" -> "The Big Lebowski")))
    ls.latest.size should be(3)
    ls.latest.values.filter(_.totalFileCount > 2).foreach(as =>
      as.findFile("film.txt").get.content should equal("The Big Lebowski"))
  }

}
