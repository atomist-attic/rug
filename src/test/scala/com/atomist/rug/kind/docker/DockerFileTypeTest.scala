package com.atomist.rug.kind.docker

import com.atomist.param.SimpleParameterValues
import com.atomist.project.edit.{ProjectEditor, SuccessfulModification}
import com.atomist.rug.{DefaultRugPipeline, TestUtils}
import com.atomist.source.{SimpleFileBasedArtifactSource, StringFileArtifact}
import org.scalatest.{FlatSpec, Matchers}

class DockerFileTypeTest extends FlatSpec with Matchers {

  it should "try docker type" in {
    val ed = TestUtils.editorInSideFile(this, "DockerUpgrade.ts")

    val target = new SimpleFileBasedArtifactSource("",
      StringFileArtifact(DockerFileType.DockerFileName,
        """
          |FROM java:8
          |
          |COPY target/service-fact-store-0.0.1-SNAPSHOT.jar /opt/build/
          |
          |WORKDIR /opt/build
          |
          |EXPOSE 8080
          |
          |ENTRYPOINT ["java", "-Xmx1g", "-jar", "service-fact-store-0.0.1-SNAPSHOT.jar"]
        """.stripMargin))
    ed.modify(target,
      SimpleParameterValues.Empty) match {
      case sm: SuccessfulModification =>
        val df = sm.result.findFile(DockerFileType.DockerFileName).get
        df.content.contains("EXPOSE 8081") should be (true)
        df.content.contains("EXPOSE 8080") should be (true)
        df.content.contains("FROM java:8-jre") should be (true)
      case _ => ???
    }
  }

  it should "try update expose" in {
    val ed = TestUtils.editorInSideFile(this, "DockerUpgrade2.ts")

    val target = new SimpleFileBasedArtifactSource("",
      StringFileArtifact(DockerFileType.DockerFileName,
        """
          |FROM java:8
          |
          |COPY target/service-fact-store-0.0.1-SNAPSHOT.jar /opt/build/
          |
          |WORKDIR /opt/build
          |
          |EXPOSE 8080
          |
          |ENTRYPOINT ["java", "-Xmx1g", "-jar", "service-fact-store-0.0.1-SNAPSHOT.jar"]
        """.stripMargin))
    ed.modify(target,
      SimpleParameterValues.Empty) match {
      case sm: SuccessfulModification =>
        val df = sm.result.findFile(DockerFileType.DockerFileName).get
        df.content.contains("EXPOSE 8081") should be (true)
        df.content.contains("EXPOSE 8080") should be (false)
        df.content.contains("FROM java:8-jre") should be (true)
        df.content.contains("HEALTHCHECK --interval=5s --timeout=3s CMD curl --fail http://localhost:8080/ || exit 1") should be (true)
      case _ => ???
    }
  }

  it should "try update expose on file not in default location" in {
    val ed = TestUtils.editorInSideFile(this, "DockerUpgrade3.ts")

    val target = new SimpleFileBasedArtifactSource("",
      StringFileArtifact("src/main/docker/" + DockerFileType.DockerFileName,
        """
          |FROM java:8
          |
          |COPY target/service-fact-store-0.0.1-SNAPSHOT.jar /opt/build/
          |
          |WORKDIR /opt/build
          |
          |EXPOSE 8080
          |
          |ENTRYPOINT ["java", "-Xmx1g", "-jar", "service-fact-store-0.0.1-SNAPSHOT.jar"]
        """.stripMargin))
    ed.modify(target,
      SimpleParameterValues.Empty) match {
      case sm: SuccessfulModification =>
        val df = sm.result.findFile("src/main/docker/" + DockerFileType.DockerFileName).get
        df.content.contains("EXPOSE 8181") should be (true)
        df.content.contains("EXPOSE 8080") should be (false)
        df.content.contains("FROM java:8-jre") should be (true)
      case _ => ???
    }
  }
}
