package com.atomist.rug.kind.docker

import com.atomist.rug.kind.docker.DockerfileParser.parse
import org.scalatest.{FlatSpec, Matchers}

class DockerfileParserTest extends FlatSpec with Matchers {

  it should "parse empty file" in {
    assert(parse(null).toString === "")
  }

  it should "parse simple file" in {
    assert(parse("FROM java:8-jre").toString === "FROM java:8-jre")
  }

  it should "parse multiline file" in {
    val parsed = parse("# test\nFROM java:8-jre\n\nRUN test \\ \n\ttest1 \\ \n\ttest2\n\nADD test.txt test.txtww\n\nEXPOSE 8080").toString
    parsed should equal("# test\nFROM java:8-jre\n\nRUN test \\\n\ttest1 \\\n\ttest2\n\nADD test.txt test.txtww\n\nEXPOSE 8080".replaceAll("\\n",System.lineSeparator()))
  }

  it should "parse a dockerfile with a list of EXPOSE ports in a single line" in {
    val parsed = parse("# test\nFROM java:8-jre\n\nRUN test \\ \n\ttest1 \\ \n\ttest2\n\nADD test.txt test.txtww\n\nEXPOSE 8080 8090").toString
    parsed should equal("# test\nFROM java:8-jre\n\nRUN test \\\n\ttest1 \\\n\ttest2\n\nADD test.txt test.txtww\n\nEXPOSE 8080 8090".replaceAll("\\n",System.lineSeparator()))
  }

  it should "parse a dockerfile with multiple EXPOSE lines" in {
    val parsed = parse("# test\nFROM java:8-jre\n\nRUN test \\ \n\ttest1 \\ \n\ttest2\n\nADD test.txt test.txtww\n\nEXPOSE 8080\nEXPOSE 8090").toString
    parsed should equal("# test\nFROM java:8-jre\n\nRUN test \\\n\ttest1 \\\n\ttest2\n\nADD test.txt test.txtww\n\nEXPOSE 8080\nEXPOSE 8090".replaceAll("\\n",System.lineSeparator()))
  }

  it should "parse a docker file with multiple lines in the LABEL section" in {
    val parsed = parse("FROM python:3.5\n\nLABEL com.atomist.line1=\"line1\" \\\n      com.atomist.line2=\"line2\"\n\nRUN test \\\n\ttest1 \\\n\ttest2\n\nENTRYPOINT python -v").toString
    parsed should include("com.atomist.line1=\"line1\"")
    parsed should include("com.atomist.line2=\"line2\"")
  }
}
