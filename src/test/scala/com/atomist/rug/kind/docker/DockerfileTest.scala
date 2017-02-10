package com.atomist.rug.kind.docker

import org.scalatest.{FlatSpec, Matchers}

class DockerfileTest extends FlatSpec with Matchers {

  import Dockerfile._

  "stringOrArrayToString" should "do nothing to a string" in {
    assert(stringOrArrayToString(Left("Paul Simon")) === "Paul Simon")
  }

  it should "do handle an empty string" in {
    assert(stringOrArrayToString(Left("")) === "")
  }

  it should "do convert a sequence to a Dockerfile string" in {
    assert(stringOrArrayToString(Right(Seq("Paul Simon", "Graceland", "1986"))) === """[ "Paul Simon", "Graceland", "1986" ]""")
  }

  "seqStringOrArrayToString" should "stringify a sequence of strings and seq of strings" in {
    val ess: Seq[Either[String, Seq[String]]] = Seq(
      Right(Seq("Paul", "Simon")),
      Left("Graceland"),
      Left("1986"),
      Right(Seq("Warner", "Bros."))
    )
    val expected: Seq[String] = Seq(
      """[ "Paul", "Simon" ]""",
      "Graceland",
      "1986",
      """[ "Warner", "Bros." ]"""
    )
    assert(seqStringOrArrayToString(ess) === expected)
  }

  "Dockerfile" should "parse new file" in {
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
    val ports = df.getExposePorts
    ports should equal(Set(8080))
  }

  it should "get single port via EXPOSE with odd spaces" in {
    val initialFile: String =
      """FROM java:8-jre
        |MAINTAINER cd@atomist.com
        |EXPOSE  8080  """.stripMargin

    val df = DockerfileParser.parse(initialFile)
    val ports = df.getExposePorts
    ports should equal(Set(8080))
  }

  it should "get a list of ports from a single EXPOSE line" in {
    val initialFile: String =
      """FROM java:8-jre
        |MAINTAINER cd@atomist.com
        |EXPOSE 8080 9090""".stripMargin

    val df = DockerfileParser.parse(initialFile)
    val ports = df.getExposePorts
    ports should equal(Set(8080, 9090))
  }

  it should "get a list of ports from a single EXPOSE line with random spaces" in {
    val initialFile: String =
      """FROM java:8-jre
        |MAINTAINER cd@atomist.com
        |EXPOSE 8080    9090  """.stripMargin

    val df = DockerfileParser.parse(initialFile)
    val ports = df.getExposePorts
    ports should equal(Set(8080, 9090))
  }

  it should "get a list of ports from multiple EXPOSE lines" in {
    val initialFile: String =
      """FROM java:8-jre
        |MAINTAINER cd@atomist.com
        |EXPOSE 8080
        |EXPOSE 9090""".stripMargin

    val df = DockerfileParser.parse(initialFile)
    val ports = df.getExposePorts
    ports should equal(Set(8080, 9090))
  }

  it should "return the value of FROM" in {
    val initialFile: String =
      """FROM java:8-jre
        |MAINTAINER cd@atomist.com
        |EXPOSE 8080
        |""".stripMargin

    val df = DockerfileParser.parse(initialFile)
    assert(df.getFrom === Some("java:8-jre"))
  }

  it should "return the value of a complicated FROM" in {
    val initialFile: String =
      """FROM docker-registry.tool.com/undertow/sober:3
        |MAINTAINER cd@atomist.com
        |EXPOSE 8080
        |""".stripMargin

    val df = DockerfileParser.parse(initialFile)
    assert(df.getFrom === Some("docker-registry.tool.com/undertow/sober:3"))
  }

  it should "return the value of a FROM with crazy whitespace" in {
    val theFinalFrontier = "  	  "
    val initialFile: String =
      s"""FROM${theFinalFrontier}docker-registry.tool.com/undertow/sober:3$theFinalFrontier
         |MAINTAINER cd@atomist.com
         |EXPOSE 8080
         |""".stripMargin

    val df = DockerfileParser.parse(initialFile)
    assert(df.getFrom === Some("docker-registry.tool.com/undertow/sober:3"))
  }

  it should "return the none if no FROM" in {
    val initialFile: String =
      """MAINTAINER cd@atomist.com
        |EXPOSE 8080
        |""".stripMargin

    val df = DockerfileParser.parse(initialFile)
    assert(df.getFrom === None)
  }

  it should "return the value of MAINTAINER" in {
    val initialFile: String =
      """FROM java:8-jre
        |MAINTAINER paul-simon@graceland.com
        |EXPOSE 8080
        |""".stripMargin

    val df = DockerfileParser.parse(initialFile)
    assert(df.getMaintainer === Some("paul-simon@graceland.com"))
  }

  it should "return the none if no MAINTAINER" in {
    val initialFile: String =
      """FROM java:8-jre
        |EXPOSE 8080
        |""".stripMargin

    val df = DockerfileParser.parse(initialFile)
    assert(df.getMaintainer === None)
  }

  it should "return a None if no LABELs" in {
    val initialFile: String =
      """FROM java:8-jre
        |MAINTAINER paul-simon@graceland.com
        |EXPOSE 8080
        |""".stripMargin

    val df = DockerfileParser.parse(initialFile)
    assert(df.getLabels === Map())
  }

  it should "return a single LABEL" in {
    val initialFile: String =
      """FROM java:8-jre
        |MAINTAINER paul-simon@graceland.com
        |LABEL mississippi-delta="shining like a National guitar"
        |EXPOSE 8080
        |""".stripMargin

    val df = DockerfileParser.parse(initialFile)
    assert(df.getLabels === Map("mississippi-delta" -> """"shining like a National guitar""""))
  }

  it should "return a simple LABEL" in {
    val initialFile: String =
      """FROM java:8-jre
        |MAINTAINER paul-simon@graceland.com
        |LABEL mississippi-delta=shining
        |EXPOSE 8080
        |""".stripMargin

    val df = DockerfileParser.parse(initialFile)
    assert(df.getLabels === Map("mississippi-delta" -> "shining"))
  }

  it should "return multiple LABELs" in {
    val initialFile: String =
      """FROM java:8-jre
        |MAINTAINER paul-simon@graceland.com
        |LABEL mississippi-delta="shining like a National guitar" traveling_companion="nine years old"
        |EXPOSE 8080
        |""".stripMargin

    val df = DockerfileParser.parse(initialFile)
    assert(df.getLabels === Map("mississippi-delta" -> """"shining like a National guitar"""",
      "traveling_companion" -> """"nine years old""""))
  }

  it should "return LABELs across line breaks" in {
    val initialFile: String =
      """FROM java:8-jre
        |MAINTAINER paul-simon@graceland.com
        |LABEL mississippi-delta="shining like a National guitar" \
        |      traveling_companion="nine years old"
        |EXPOSE 8080
        |""".stripMargin

    val df = DockerfileParser.parse(initialFile)
    assert(df.getLabels === Map("mississippi-delta" -> """"shining like a National guitar"""",
      "traveling_companion" -> """"nine years old""""))
  }

  it should "return labels from multiple LABEL statements" in {
    val initialFile: String =
      """FROM java:8-jre
        |MAINTAINER paul-simon@graceland.com
        |LABEL mississippi-delta="shining like a National guitar"
        |LABEL traveling_companion="nine years old"
        |EXPOSE 8080
        |""".stripMargin

    val df = DockerfileParser.parse(initialFile)
    assert(df.getLabels === Map("mississippi-delta" -> """"shining like a National guitar"""",
      "traveling_companion" -> """"nine years old""""))
  }

  it should "return the value of the last LABEL" in {
    val initialFile: String =
      """FROM java:8-jre
        |MAINTAINER paul-simon@graceland.com
        |LABEL mississippi-delta="shining like a Mule guitar"
        |LABEL mississippi-delta="shining like a National guitar"
        |EXPOSE 8080
        |""".stripMargin

    val df = DockerfileParser.parse(initialFile)
    assert(df.getLabels === Map("mississippi-delta" -> """"shining like a National guitar""""))
  }

  it should "return an empty seq when there is no CMD" in {
    val initialFile: String =
      """FROM java:8-jre
        |MAINTAINER paul-simon@graceland.com
        |LABEL mississippi-delta="shining like a National guitar"
        |EXPOSE 8080
        |""".stripMargin

    val df = DockerfileParser.parse(initialFile)
    assert(df.getCmd === Right(Seq()))
  }

  it should "return the string value of CMD" in {
    val initialFile: String =
      """FROM java:8-jre
        |MAINTAINER paul-simon@graceland.com
        |LABEL mississippi-delta="shining like a National guitar"
        |EXPOSE 8080
        |CMD simple string cmd argument
        |""".stripMargin

    val df = DockerfileParser.parse(initialFile)
    assert(df.getCmd === Left("simple string cmd argument"))
  }

  it should "return the string value of CMD across two lines" in {
    val initialFile: String =
      """FROM java:8-jre
        |MAINTAINER paul-simon@graceland.com
        |LABEL mississippi-delta="shining like a National guitar"
        |EXPOSE 8080
        |CMD simple string cmd argument \
        |    that spans two lines
        |""".stripMargin

    val df = DockerfileParser.parse(initialFile)
    // unfortunate the parser does not retain formatting.
    assert(df.getCmd === Left("simple string cmd argument     that spans two lines"))
  }

  it should "return the array value of CMD" in {
    val initialFile: String =
      """FROM java:8-jre
        |MAINTAINER paul-simon@graceland.com
        |LABEL mississippi-delta="shining like a National guitar"
        |EXPOSE 8080
        |CMD [ "array", "of", "strings", "cmd", "argument" ]
        |""".stripMargin

    val df = DockerfileParser.parse(initialFile)
    assert(df.getCmd === Right(Seq("array", "of", "strings", "cmd", "argument")))
  }

  it should "return the very long array value of CMD" in {
    val initialFile: String =
      """FROM java:8-jre
        |MAINTAINER paul-simon@graceland.com
        |LABEL mississippi-delta="shining like a National guitar"
        |EXPOSE 8080
        |CMD [ "array", "of", "strings", "cmd", "argument", "that", "is", "long", "enough", "to", "make", "sure", "sorting", "is", "not", "using", "strings" ]
        |""".stripMargin

    val df = DockerfileParser.parse(initialFile)
    assert(df.getCmd === Right(Seq("array", "of", "strings", "cmd", "argument", "that", "is", "long", "enough", "to", "make", "sure", "sorting", "is", "not", "using", "strings")))
  }

  it should "return an empty seq when there is no ENTRYPOINT" in {
    val initialFile: String =
      """FROM java:8-jre
        |MAINTAINER paul-simon@graceland.com
        |LABEL mississippi-delta="shining like a National guitar"
        |EXPOSE 8080
        |CMD simple string cmd argument
        |""".stripMargin

    val df = DockerfileParser.parse(initialFile)
    assert(df.getEntryPoint === Right(Seq()))
  }

  it should "return the string value of ENTRYPOINT" in {
    val initialFile: String =
      """FROM java:8-jre
        |MAINTAINER paul-simon@graceland.com
        |LABEL mississippi-delta="shining like a National guitar"
        |EXPOSE 8080
        |CMD simple string cmd argument
        |ENTRYPOINT simple string entry point argument
        |""".stripMargin

    val df = DockerfileParser.parse(initialFile)
    assert(df.getEntryPoint === Left("simple string entry point argument"))
  }

  it should "return the array value of ENTRYPOINT" in {
    val initialFile: String =
      """FROM java:8-jre
        |MAINTAINER paul-simon@graceland.com
        |LABEL mississippi-delta="shining like a National guitar"
        |EXPOSE 8080
        |CMD [ "array", "of", "strings", "cmd", "argument" ]
        |ENTRYPOINT [ "array", "of", "strings", "entry", "point", "argument" ]
        |""".stripMargin

    val df = DockerfileParser.parse(initialFile)
    assert(df.getEntryPoint === Right(Seq("array", "of", "strings", "entry", "point", "argument")))
  }

  it should "return the value of none HEALTHCHECK" in {
    val initialFile: String =
      """FROM java:8-jre
        |MAINTAINER paul-simon@graceland.com
        |EXPOSE 8080
        |HEALTHCHECK NONE
        |""".stripMargin

    val df = DockerfileParser.parse(initialFile)
    assert(df.getHealthCheck === Some("NONE"))
  }

  it should "return the value of simple HEALTHCHECK" in {
    val initialFile: String =
      """FROM java:8-jre
        |MAINTAINER paul-simon@graceland.com
        |EXPOSE 8080
        |HEALTHCHECK CMD run this
        |""".stripMargin

    val df = DockerfileParser.parse(initialFile)
    assert(df.getHealthCheck === Some("CMD run this"))
  }

  it should "return the value of HEALTHCHECK with options" in {
    val initialFile: String =
      """FROM java:8-jre
        |MAINTAINER paul-simon@graceland.com
        |EXPOSE 8080
        |HEALTHCHECK --interval=20s --timeout=10s CMD run this
        |""".stripMargin

    val df = DockerfileParser.parse(initialFile)
    assert(df.getHealthCheck === Some("--interval=20s --timeout=10s CMD run this"))
  }

  it should "return the value of HEALTHCHECK with command array" in {
    val initialFile: String =
      """FROM java:8-jre
        |MAINTAINER paul-simon@graceland.com
        |EXPOSE 8080
        |HEALTHCHECK --interval=5s CMD [ "run", "this" ]
        |""".stripMargin

    val df = DockerfileParser.parse(initialFile)
    assert(df.getHealthCheck === Some("""--interval=5s CMD [ "run", "this" ]"""))
  }

  it should "return the none if no HEALTHCHECK" in {
    val initialFile: String =
      """FROM java:8-jre
        |EXPOSE 8080
        |""".stripMargin

    val df = DockerfileParser.parse(initialFile)
    assert(df.getHealthCheck === None)
  }

  it should "return an empty seq when there is no RUN" in {
    val initialFile: String =
      """FROM java:8-jre
        |MAINTAINER paul-simon@graceland.com
        |LABEL mississippi-delta="shining like a National guitar"
        |EXPOSE 8080
        |CMD simple string cmd argument
        |""".stripMargin

    val df = DockerfileParser.parse(initialFile)
    assert(df.getRuns === Seq())
  }

  it should "return the string value of RUN" in {
    val initialFile: String =
      """FROM java:8-jre
        |MAINTAINER paul-simon@graceland.com
        |LABEL mississippi-delta="shining like a National guitar"
        |RUN human trampoline
        |EXPOSE 8080
        |CMD simple string cmd argument
        |ENTRYPOINT simple string entry point argument
        |""".stripMargin

    val df = DockerfileParser.parse(initialFile)
    assert(df.getRuns === Seq(Left("human trampoline")))
  }

  it should "return the array value of RUN" in {
    val initialFile: String =
      """FROM java:8-jre
        |MAINTAINER paul-simon@graceland.com
        |LABEL mississippi-delta="shining like a National guitar"
        |EXPOSE 8080
        |RUN [ "falling", "tumbling", "turmoil" ]
        |CMD [ "array", "of", "strings", "cmd", "argument" ]
        |ENTRYPOINT [ "array", "of", "strings", "entry", "point", "argument" ]
        |""".stripMargin

    val df = DockerfileParser.parse(initialFile)
    assert(df.getRuns === Seq(Right(Seq("falling", "tumbling", "turmoil"))))
  }

  it should "return the all the right values of RUN" in {
    val initialFile: String =
      """FROM java:8-jre
        |MAINTAINER paul-simon@graceland.com
        |LABEL mississippi-delta="shining like a National guitar"
        |RUN human trampoline
        |EXPOSE 8080
        |RUN [ "falling", "tumbling", "turmoil" ]
        |CMD [ "array", "of", "strings", "cmd", "argument" ]
        |ENTRYPOINT [ "array", "of", "strings", "entry", "point", "argument" ]
        |""".stripMargin

    val df = DockerfileParser.parse(initialFile)
    assert(df.getRuns === Seq(Left("human trampoline"), Right(Seq("falling", "tumbling", "turmoil"))))
  }

  it should "return an empty seq when there is no COPY" in {
    val initialFile: String =
      """FROM java:8-jre
        |MAINTAINER paul-simon@graceland.com
        |LABEL mississippi-delta="shining like a National guitar"
        |EXPOSE 8080
        |CMD simple string cmd argument
        |""".stripMargin

    val df = DockerfileParser.parse(initialFile)
    assert(df.getCopies === Seq())
  }

  it should "return the string value of COPY" in {
    val initialFile: String =
      """FROM java:8-jre
        |MAINTAINER paul-simon@graceland.com
        |LABEL mississippi-delta="shining like a National guitar"
        |RUN human trampoline
        |EXPOSE 8080
        |COPY traveling companions
        |CMD simple string cmd argument
        |ENTRYPOINT simple string entry point argument
        |""".stripMargin

    val df = DockerfileParser.parse(initialFile)
    assert(df.getCopies === Seq(Seq("traveling", "companions")))
  }

  it should "return the array value of COPY" in {
    val initialFile: String =
      """FROM java:8-jre
        |MAINTAINER paul-simon@graceland.com
        |LABEL mississippi-delta="shining like a National guitar"
        |EXPOSE 8080
        |COPY [ "ghosts", "and", "empty", "sockets" ]
        |RUN [ "falling", "tumbling", "turmoil" ]
        |CMD [ "array", "of", "strings", "cmd", "argument" ]
        |ENTRYPOINT [ "array", "of", "strings", "entry", "point", "argument" ]
        |""".stripMargin

    val df = DockerfileParser.parse(initialFile)
    assert(df.getCopies === Seq(Seq("ghosts", "and", "empty", "sockets")))
  }

  it should "return the all the right values of COPY" in {
    val initialFile: String =
      """FROM java:8-jre
        |MAINTAINER paul-simon@graceland.com
        |LABEL mississippi-delta="shining like a National guitar"
        |RUN human trampoline
        |EXPOSE 8080
        |COPY traveling companions
        |COPY [ "ghosts", "and", "empty", "sockets" ]
        |RUN [ "falling", "tumbling", "turmoil" ]
        |CMD [ "array", "of", "strings", "cmd", "argument" ]
        |ENTRYPOINT [ "array", "of", "strings", "entry", "point", "argument" ]
        |""".stripMargin

    val df = DockerfileParser.parse(initialFile)
    assert(df.getCopies === Seq(Seq("traveling", "companions"), Seq("ghosts", "and", "empty", "sockets")))
  }

  it should "return an empty seq when there is no ADD" in {
    val initialFile: String =
      """FROM java:8-jre
        |MAINTAINER paul-simon@graceland.com
        |LABEL mississippi-delta="shining like a National guitar"
        |EXPOSE 8080
        |CMD simple string cmd argument
        |""".stripMargin

    val df = DockerfileParser.parse(initialFile)
    assert(df.getAdds === Seq())
  }

  it should "return the string value of ADD" in {
    val initialFile: String =
      """FROM java:8-jre
        |MAINTAINER paul-simon@graceland.com
        |LABEL mississippi-delta="shining like a National guitar"
        |RUN human trampoline
        |EXPOSE 8080
        |COPY traveling companions
        |ADD poor boys
        |CMD simple string cmd argument
        |ENTRYPOINT simple string entry point argument
        |""".stripMargin

    val df = DockerfileParser.parse(initialFile)
    assert(df.getAdds === Seq(Seq("poor", "boys")))
  }

  it should "return the array value of ADD" in {
    val initialFile: String =
      """FROM java:8-jre
        |MAINTAINER paul-simon@graceland.com
        |LABEL mississippi-delta="shining like a National guitar"
        |EXPOSE 8080
        |ADD [ "pilgrims", "with", "families" ]
        |COPY [ "ghosts", "and", "empty", "sockets" ]
        |RUN [ "falling", "tumbling", "turmoil" ]
        |CMD [ "array", "of", "strings", "cmd", "argument" ]
        |ENTRYPOINT [ "array", "of", "strings", "entry", "point", "argument" ]
        |""".stripMargin

    val df = DockerfileParser.parse(initialFile)
    assert(df.getAdds === Seq(Seq("pilgrims", "with", "families")))
  }

  it should "return the all the right values of ADD" in {
    val initialFile: String =
      """FROM java:8-jre
        |MAINTAINER paul-simon@graceland.com
        |LABEL mississippi-delta="shining like a National guitar"
        |RUN human trampoline
        |EXPOSE 8080
        |ADD poor boys
        |COPY traveling companions
        |COPY [ "ghosts", "and", "empty", "sockets" ]
        |ADD [ "pilgrims", "with", "families" ]
        |RUN [ "falling", "tumbling", "turmoil" ]
        |CMD [ "array", "of", "strings", "cmd", "argument" ]
        |ENTRYPOINT [ "array", "of", "strings", "entry", "point", "argument" ]
        |""".stripMargin

    val df = DockerfileParser.parse(initialFile)
    assert(df.getAdds === Seq(Seq("poor", "boys"), Seq("pilgrims", "with", "families")))
  }

  it should "return an empty seq when there is no VOLUME" in {
    val initialFile: String =
      """FROM java:8-jre
        |MAINTAINER paul-simon@graceland.com
        |LABEL mississippi-delta="shining like a National guitar"
        |EXPOSE 8080
        |CMD simple string cmd argument
        |""".stripMargin

    val df = DockerfileParser.parse(initialFile)
    assert(df.getVolumes === Seq())
  }

  it should "return the string value of VOLUME" in {
    val initialFile: String =
      """FROM java:8-jre
        |MAINTAINER paul-simon@graceland.com
        |LABEL mississippi-delta="shining like a National guitar"
        |RUN human trampoline
        |EXPOSE 8080
        |VOLUME /losing /love
        |COPY traveling companions
        |ADD poor boys
        |CMD simple string cmd argument
        |ENTRYPOINT simple string entry point argument
        |""".stripMargin

    val df = DockerfileParser.parse(initialFile)
    assert(df.getVolumes === Seq(Seq("/losing", "/love")))
  }

  it should "return the array value of VOLUME" in {
    val initialFile: String =
      """FROM java:8-jre
        |MAINTAINER paul-simon@graceland.com
        |LABEL mississippi-delta="shining like a National guitar"
        |EXPOSE 8080
        |ADD [ "pilgrims", "with", "families" ]
        |COPY [ "ghosts", "and", "empty", "sockets" ]
        |VOLUME [ "/like/a/window", "/in/your/heart" ]
        |RUN [ "falling", "tumbling", "turmoil" ]
        |CMD [ "array", "of", "strings", "cmd", "argument" ]
        |ENTRYPOINT [ "array", "of", "strings", "entry", "point", "argument" ]
        |""".stripMargin

    val df = DockerfileParser.parse(initialFile)
    assert(df.getVolumes === Seq(Seq("/like/a/window", "/in/your/heart")))
  }

  it should "return the all the right values of VOLUME" in {
    val initialFile: String =
      """FROM java:8-jre
        |MAINTAINER paul-simon@graceland.com
        |LABEL mississippi-delta="shining like a National guitar"
        |RUN human trampoline
        |EXPOSE 8080
        |ADD poor boys
        |COPY traveling companions
        |COPY [ "ghosts", "and", "empty", "sockets" ]
        |ADD [ "pilgrims", "with", "families" ]
        |VOLUME [ "/like/a/window", "/in/your/heart" ]
        |VOLUME /losing /love
        |RUN [ "falling", "tumbling", "turmoil" ]
        |CMD [ "array", "of", "strings", "cmd", "argument" ]
        |ENTRYPOINT [ "array", "of", "strings", "entry", "point", "argument" ]
        |""".stripMargin

    val df = DockerfileParser.parse(initialFile)
    assert(df.getVolumes === Seq(Seq("/like/a/window", "/in/your/heart"), Seq("/losing", "/love")))
  }

  it should "return the value of simple WORKDIR" in {
    val initialFile: String =
      """FROM java:8-jre
        |MAINTAINER paul-simon@graceland.com
        |WORKDIR /opt/graceland
        |EXPOSE 8080
        |HEALTHCHECK CMD run this
        |""".stripMargin

    val df = DockerfileParser.parse(initialFile)
    assert(df.getWorkdir === Some("/opt/graceland"))
  }

  it should "return the value of the last WORKDIR" in {
    val initialFile: String =
      """FROM java:8-jre
        |MAINTAINER paul-simon@graceland.com
        |WORKDIR /opt/hearts-and-bones
        |WORKDIR /opt/graceland
        |EXPOSE 8080
        |HEALTHCHECK --interval=5s CMD [ "run", "this" ]
        |""".stripMargin

    val df = DockerfileParser.parse(initialFile)
    assert(df.getWorkdir === Some("/opt/graceland"))
  }

  it should "return the none if no WORKDIR" in {
    val initialFile: String =
      """FROM java:8-jre
        |EXPOSE 8080
        |""".stripMargin

    val df = DockerfileParser.parse(initialFile)
    assert(df.getWorkdir === None)
  }

  it should "return a None if no ENVs" in {
    val initialFile: String =
      """FROM java:8-jre
        |MAINTAINER paul-simon@graceland.com
        |EXPOSE 8080
        |""".stripMargin

    val df = DockerfileParser.parse(initialFile)
    assert(df.getEnvs === Map())
  }

  it should "return a single ENV" in {
    val initialFile: String =
      """FROM java:8-jre
        |MAINTAINER paul-simon@graceland.com
        |LABEL mississippi-delta="shining like a National guitar"
        |EXPOSE 8080
        |ENV MEMPHIS TN
        |""".stripMargin

    val df = DockerfileParser.parse(initialFile)
    assert(df.getEnvs === Map("MEMPHIS" -> "TN"))
  }

  it should "return a simple ENV" in {
    val initialFile: String =
      """FROM java:8-jre
        |MAINTAINER paul-simon@graceland.com
        |LABEL mississippi-delta=shining
        |EXPOSE 8080
        |ENV MEMPHIS=TN
        |""".stripMargin

    val df = DockerfileParser.parse(initialFile)
    assert(df.getEnvs === Map("MEMPHIS" -> "TN"))
  }

  it should "return multiple ENVs" in {
    val initialFile: String =
      """FROM java:8-jre
        |MAINTAINER paul-simon@graceland.com
        |LABEL mississippi-delta="shining like a National guitar" traveling_companion="nine years old"
        |EXPOSE 8080
        |ENV MEMPHIS=TN GRACELAND=GRACELAND
        |""".stripMargin

    val df = DockerfileParser.parse(initialFile)
    assert(df.getEnvs === Map("MEMPHIS" -> "TN", "GRACELAND" -> "GRACELAND"))
  }

  it should "return ENVs across line breaks" in {
    val initialFile: String =
      """FROM java:8-jre
        |MAINTAINER paul-simon@graceland.com
        |LABEL mississippi-delta="shining like a National guitar" \
        |      traveling_companion="nine years old"
        |EXPOSE 8080
        |ENV MEMPHIS=TN \
        |    WE="are going to Graceland"
        |""".stripMargin

    val df = DockerfileParser.parse(initialFile)
    assert(df.getEnvs === Map("MEMPHIS" -> "TN", "WE" -> """"are going to Graceland""""))
  }

  it should "return variables from multiple ENV statements" in {
    val initialFile: String =
      """FROM java:8-jre
        |MAINTAINER paul-simon@graceland.com
        |ENV MEMPHIS=TN
        |ENV WE="are going to Graceland"
        |EXPOSE 8080
        |""".stripMargin

    val df = DockerfileParser.parse(initialFile)
    assert(df.getEnvs === Map("MEMPHIS" -> "TN", "WE" -> """"are going to Graceland""""))
  }

  it should "return the value of the last ENV" in {
    val initialFile: String =
      """FROM java:8-jre
        |MAINTAINER paul-simon@graceland.com
        |ENV MEMPHIS=EGYPT
        |ENV MEMPHIS=TN
        |EXPOSE 8080
        |""".stripMargin

    val df = DockerfileParser.parse(initialFile)
    assert(df.getEnvs === Map("MEMPHIS" -> "TN"))
  }
}
