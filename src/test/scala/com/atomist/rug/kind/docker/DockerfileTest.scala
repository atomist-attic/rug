package com.atomist.rug.kind.docker

import org.scalatest.{FlatSpec, Matchers}

class DockerfileTest extends FlatSpec with Matchers {

  it should "parse new file" in {
    val df = new Dockerfile
    df.addOrUpdateFrom("java:8-jre")
      .addMaintainer("cd@atomist.com")
      .addAdd("target-0.0.1.jar /opt/root")
      .addExpose("8080")
      .addOrUpdateEntryPoint("[\"java\"]")
      .addOrUpdateCmd("[\"-Xmx1g\", \"-jar\", \"test1-0.0.1.jar\"]")
      .addOrUpdateWorkdir("/opt/root")

    val expectedContent: String =
      """FROM java:8-jre
        |MAINTAINER cd@atomist.com
        |ADD target-0.0.1.jar /opt/root
        |EXPOSE 8080
        |WORKDIR /opt/root
        |ENTRYPOINT ["java"]
        |CMD ["-Xmx1g", "-jar", "test1-0.0.1.jar"]""".stripMargin

    assert(df.toString === expectedContent)
  }

  it should "update file" in {
    val initialFile: String =
      """FROM java:8-jre
        |MAINTAINER cd@atomist.com
        |ADD target-0.0.1.jar /opt/root
        |EXPOSE 8080
        |WORKDIR /opt/root
        |ENTRYPOINT ["java"]
        |CMD ["-Xmx1g", "-jar", "test1-0.0.1.jar"]""".stripMargin

    val df = DockerfileParser.parse(initialFile)
      .addOrUpdateFrom("java:6-jre")
      .addMaintainer("the-rug@atomist.com")
      .addOrUpdateCmd("[\"-Xmx2g\", \"-jar\", \"test1-0.0.1.jar\"]")
      .addOrUpdateWorkdir("/opt/dude")

    val expectedContent: String =
      """FROM java:6-jre
        |MAINTAINER the-rug@atomist.com
        |MAINTAINER cd@atomist.com
        |ADD target-0.0.1.jar /opt/root
        |EXPOSE 8080
        |WORKDIR /opt/dude
        |ENTRYPOINT ["java"]
        |CMD ["-Xmx2g", "-jar", "test1-0.0.1.jar"]""".stripMargin
    assert(df.toString === expectedContent)
  }

  it should "get single port via EXPOSE" in {
    val initialFile: String =
    """FROM java:8-jre
      |MAINTAINER cd@atomist.com
      |EXPOSE 8080""".stripMargin

    val df = DockerfileParser.parse(initialFile)
    val ports = df.getExposePorts()
    ports should equal(Set(8080))
  }

  it should "get single port via EXPOSE with odd spaces" in {
    val initialFile: String =
      """FROM java:8-jre
        |MAINTAINER cd@atomist.com
        |EXPOSE  8080  """.stripMargin

    val df = DockerfileParser.parse(initialFile)
    val ports = df.getExposePorts()
    ports should equal(Set(8080))
  }

  it should "get a list of ports from a single EXPOSE line" in {
    val initialFile: String =
      """FROM java:8-jre
        |MAINTAINER cd@atomist.com
        |EXPOSE 8080 9090""".stripMargin

    val df = DockerfileParser.parse(initialFile)
    val ports = df.getExposePorts()
    ports should equal(Set(8080, 9090))
  }

  it should "get a list of ports from a single EXPOSE line with random spaces" in {
    val initialFile: String =
      """FROM java:8-jre
        |MAINTAINER cd@atomist.com
        |EXPOSE 8080    9090  """.stripMargin

    val df = DockerfileParser.parse(initialFile)
    val ports = df.getExposePorts()
    ports should equal(Set(8080, 9090))
  }

  it should "get a list of ports from multiple EXPOSE lines" in {
    val initialFile: String =
      """FROM java:8-jre
        |MAINTAINER cd@atomist.com
        |EXPOSE 8080
        |EXPOSE 9090""".stripMargin

    val df = DockerfileParser.parse(initialFile)
    val ports = df.getExposePorts()
    ports should equal(Set(8080, 9090))
  }
}
