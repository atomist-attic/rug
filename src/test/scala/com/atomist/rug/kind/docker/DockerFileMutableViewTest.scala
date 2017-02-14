package com.atomist.rug.kind.docker

import com.atomist.rug.kind.core.ProjectMutableView
import com.atomist.source.{EmptyArtifactSource, SimpleFileBasedArtifactSource, StringFileArtifact}
import org.scalatest.{FlatSpec, Matchers}

import scala.collection.JavaConverters._

object DockerFileMutableViewTest {
  val dockerFile =
    """FROM java:8-jre
      |MAINTAINER nobody@java.org
      |WORKDIR /opt/hearts-and-bones
      |FROM simon.garfunkel.org/central-park:1981
      |MAINTAINER paul-simon@graceland.com
      |WORKDIR /opt/graceland
      |LABEL mississippi-delta="shining like a National guitar" \
      |      traveling_companion="nine years old"
      |RUN human trampoline
      |LABEL following=theRiver
      |EXPOSE 8080 9090
      |EXPOSE 8081
      |ENV GRACELAND Graceland
      |ENV MEMPHIS=TN \
      |    IM="going to Graceland"
      |ADD poor boys
      |COPY traveling companions
      |COPY [ "ghosts", "and", "empty", "sockets" ]
      |ADD [ "pilgrims", "with", "families" ]
      |ENV WE="are going to Graceland" \
      |    MEMPHIS=Tennessee
      |VOLUME /losing /love
      |VOLUME [ "/like/a/window", "/in/your/heart" ]
      |HEALTHCHECK --interval=20s --timeout=10s CMD [ "everybody", "sees", "you're" ]
      |HEALTHCHECK --retries=5 CMD blown apart
      |RUN [ "falling", "tumbling", "turmoil" ]
      |CMD [ "I've", "reason", "to", "believe" ]
      |CMD we all will be received
      |ENTRYPOINT in Graceland
      |ENTRYPOINT [ "no", "not", "you", "my", "brother" ]
      |""".stripMargin

  val df = StringFileArtifact("docker/Dockerfile", dockerFile)

  val as = new SimpleFileBasedArtifactSource("docker-project", Seq(df))

  val pmv = new ProjectMutableView(EmptyArtifactSource(""), as)

  val dfmv = new DockerFileMutableView(df, pmv)
}

class DockerFileMutableViewTest extends FlatSpec with Matchers {

  import DockerFileMutableViewTest._

  it should "return the right FROM" in {
    assert(dfmv.getFrom === "simon.garfunkel.org/central-park:1981")
  }

  it should "return all the exposed ports" in {
    val expected: java.util.List[Int] = Seq(8080, 9090, 8081).asJava
    assert(dfmv.getExposedPorts === expected)
  }

  it should "return the right MAINTAINER" in {
    assert(dfmv.getMaintainer === "paul-simon@graceland.com")
  }

  it should "return all the LABELs" in {
    val expected: Map[String, String] = Map(
      "mississippi-delta" -> "\"shining like a National guitar\"",
      "traveling_companion" -> "\"nine years old\"",
      "following" -> "theRiver"
    )
    assert(dfmv.getLabels === expected)
  }

  it should "return all the RUNs" in {
    val expected: Seq[String] = Seq(
      "human trampoline",
      """[ "falling", "tumbling", "turmoil" ]"""
    )
    assert(dfmv.getRuns === expected)
  }

  it should "return all the COPYs" in {
    val expected: Seq[Seq[String]] = Seq(
      Seq("traveling", "companions"),
      Seq("ghosts", "and", "empty", "sockets")
    )
    assert(dfmv.getCopies === expected)
  }

  it should "return all the ADDs" in {
    val expected: Seq[Seq[String]] = Seq(
      Seq("poor", "boys"),
      Seq("pilgrims", "with", "families")
    )
    assert(dfmv.getAdds === expected)
  }

  it should "return all the environment variables" in {
    val expected: Map[String, String] = Map(
      "GRACELAND" -> "Graceland",
      "IM" -> "\"going to Graceland\"",
      "WE" -> "\"are going to Graceland\"",
      "MEMPHIS" -> "Tennessee"
    )
    assert(dfmv.getEnvs === expected)
  }

  it should "return all the VOLUMEs" in {
    val expected: Seq[Seq[String]] = Seq(
      Seq("/losing", "/love"),
      Seq("/like/a/window", "/in/your/heart")
    )
    assert(dfmv.getVolumes === expected)
  }

  it should "return the right WORKDIR" in {
    assert(dfmv.getWorkdir === "/opt/graceland")
  }

  it should "return the right ENTRYPOINT" in {
    assert(dfmv.getEntryPoint === """[ "no", "not", "you", "my", "brother" ]""")
  }

  it should "return the right CMD" in {
    assert(dfmv.getCmd === "we all will be received")
  }

  it should "return the right HEALTHCHECK" in {
    assert(dfmv.getHealthcheck === "--retries=5 CMD blown apart")
  }
}
