package com.atomist.rug.kind.docker

import com.atomist.project.SimpleProjectOperationArguments
import com.atomist.project.edit.{ProjectEditor, SuccessfulModification}
import com.atomist.rug.DefaultRugPipeline
import com.atomist.source.{SimpleFileBasedArtifactSource, StringFileArtifact}
import org.scalatest.{FlatSpec, Matchers}

class DockerTypeTest extends FlatSpec with Matchers {

  it should "try docker type" in {
    val prog =
      """
        |editor DockerUpgrade
        |
        |with dockerfile d
        |begin
        |	do addExpose "8081"
        | do addOrUpdateFrom "java:8-jre"
        |end
      """.stripMargin
    val rp = new DefaultRugPipeline
    val ed = rp.createFromString(prog).head.asInstanceOf[ProjectEditor]

    val target = new SimpleFileBasedArtifactSource("",
      StringFileArtifact(DockerType.DockerFileName,
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
      SimpleProjectOperationArguments.Empty) match {
      case sm: SuccessfulModification =>
        val df = sm.result.findFile(DockerType.DockerFileName).get
        df.content.contains("EXPOSE 8081") should be (true)
        df.content.contains("EXPOSE 8080") should be (true)
        df.content.contains("FROM java:8-jre") should be (true)
    }
  }

  it should "try update expose" in {
    val prog =
      """
        |editor DockerUpgrade
        |
        |with dockerfile d
        |begin
        |	do addOrUpdateExpose "8081"
        | do addOrUpdateFrom "java:8-jre"
        |end
      """.stripMargin
    val rp = new DefaultRugPipeline
    val ed = rp.createFromString(prog).head.asInstanceOf[ProjectEditor]

    val target = new SimpleFileBasedArtifactSource("",
      StringFileArtifact(DockerType.DockerFileName,
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
      SimpleProjectOperationArguments.Empty) match {
      case sm: SuccessfulModification =>
        val df = sm.result.findFile(DockerType.DockerFileName).get
        df.content.contains("EXPOSE 8081") should be (true)
        df.content.contains("EXPOSE 8080") should be (false)
        df.content.contains("FROM java:8-jre") should be (true)
    }
  }

  it should "try update expose on file not in default location" in {
    val prog =
      """
        |editor DockerUpgrade
        |
        |let exposePort = "8181"
        |
        |with dockerfile d
        |begin
        |	do addOrUpdateExpose exposePort
        | do addOrUpdateFrom "java:8-jre"
        |end
      """.stripMargin
    val rp = new DefaultRugPipeline
    val ed = rp.createFromString(prog).head.asInstanceOf[ProjectEditor]

    val target = new SimpleFileBasedArtifactSource("",
      StringFileArtifact("src/main/docker/" + DockerType.DockerFileName,
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
      SimpleProjectOperationArguments.Empty) match {
      case sm: SuccessfulModification =>
        val df = sm.result.findFile("src/main/docker/" + DockerType.DockerFileName).get
        df.content.contains("EXPOSE 8181") should be (true)
        df.content.contains("EXPOSE 8080") should be (false)
        df.content.contains("FROM java:8-jre") should be (true)
    }
  }
}
