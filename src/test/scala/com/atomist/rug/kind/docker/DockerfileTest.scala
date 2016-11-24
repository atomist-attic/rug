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

    df.toString should equal("FROM java:8-jre\nMAINTAINER cd@atomist.com\nADD target-0.0.1.jar /opt/root\nEXPOSE 8080\nWORKDIR /opt/root\nENTRYPOINT [\"java\"]\nCMD [\"-Xmx1g\", \"-jar\", \"test1-0.0.1.jar\"]")
  }

  it should "update file" in {
    val df = DockerfileParser.parse("FROM java:8-jre\nMAINTAINER cd@atomist.com\nADD target-0.0.1.jar /opt/root\nEXPOSE 8080\nWORKDIR /opt/root\nENTRYPOINT [\"java\"]\nCMD [\"-Xmx1g\", \"-jar\", \"test1-0.0.1.jar\"]")
      .addOrUpdateFrom("java:6-jre")
      .addMaintainer("the-rug@atomist.com")
      .addOrUpdateCmd("[\"-Xmx2g\", \"-jar\", \"test1-0.0.1.jar\"]")
      .addOrUpdateWorkdir("/opt/dude")

    df.toString should equal("FROM java:6-jre\nMAINTAINER the-rug@atomist.com\nMAINTAINER cd@atomist.com\nADD target-0.0.1.jar /opt/root\nEXPOSE 8080\nWORKDIR /opt/dude\nENTRYPOINT [\"java\"]\nCMD [\"-Xmx2g\", \"-jar\", \"test1-0.0.1.jar\"]")
  }
}
