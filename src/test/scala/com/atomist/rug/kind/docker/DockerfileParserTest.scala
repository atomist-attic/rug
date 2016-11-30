package com.atomist.rug.kind.docker

import com.atomist.rug.kind.docker.DockerfileParser.parse
import org.scalatest.{FlatSpec, Matchers}

class DockerfileParserTest extends FlatSpec with Matchers {

  it should "parse empty file" in {
    parse(null).toString should equal("")
  }

  it should "parse simple file" in {
    parse("FROM java:8-jre").toString should equal("FROM java:8-jre")
  }

  it should "parse multiline file" in {
    val parsed = parse("# test\nFROM java:8-jre\n\nRUN test \\ \n\ttest1 \\ \n\ttest2\n\nADD test.txt test.txtww\n\nEXPOSE 8080").toString
    parsed should equal("# test\nFROM java:8-jre\n\nRUN test \\\n\ttest1 \\\n\ttest2\n\nADD test.txt test.txtww\n\nEXPOSE 8080")
  }

  it should "parse a dockerfile with a list of EXPOSE ports in a single line" in {
    val parsed = parse("# test\nFROM java:8-jre\n\nRUN test \\ \n\ttest1 \\ \n\ttest2\n\nADD test.txt test.txtww\n\nEXPOSE 8080 8090").toString
    parsed should equal("# test\nFROM java:8-jre\n\nRUN test \\\n\ttest1 \\\n\ttest2\n\nADD test.txt test.txtww\n\nEXPOSE 8080 8090")
  }

  it should "parse a dockerfile with multiple EXPOSE lines" in {
    val parsed = parse("# test\nFROM java:8-jre\n\nRUN test \\ \n\ttest1 \\ \n\ttest2\n\nADD test.txt test.txtww\n\nEXPOSE 8080\nEXPOSE 8090").toString
    parsed should equal("# test\nFROM java:8-jre\n\nRUN test \\\n\ttest1 \\\n\ttest2\n\nADD test.txt test.txtww\n\nEXPOSE 8080\nEXPOSE 8090")
  }
}
