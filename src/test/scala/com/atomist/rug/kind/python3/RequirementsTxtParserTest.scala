package com.atomist.rug.kind.python3

import com.atomist.model.content.text.TreeNode
import org.scalatest.{FlatSpec, Matchers}

object RequirementsTxtParserTest {

  val simple1 =
    """
      |flask==0.11.1
      |pytest
      |pytest-flask==0.10.0
      |click==6.6
      |cherrypy==8.1.2
      |marshmallow==2.10.2
      |apispec==0.14.0
      |Flask-SQLAlchemy==2.1
    """.stripMargin

  // Taken from https://pip.pypa.io/en/stable/reference/pip_install/#requirements-file-format
  val noVersions =
  """
    |#
    |####### example-requirements.txt #######
    |#
    |###### Requirements without Version Specifiers ######
    |nose
    |nose-cov
    |beautifulsoup4
  """.stripMargin

  val withVersions = noVersions + "\n" +
    """
      |#
      |###### Requirements with Version Specifiers ######
      |#   See https://www.python.org/dev/peps/pep-0440/#version-specifiers
      |docopt == 0.6.1             # Version Matching. Must be version 0.6.1
    """.stripMargin

}

class RequirementsTxtParserTest extends FlatSpec with Matchers {

  import RequirementsTxtParserTest._

  it should "return all package names" in {
    val reqs = RequirementsTxtParser.parseFile(simple1)
    reqs.requirements.size should be > (0)
    reqs.requirements.map(_.packageName.value).contains("pytest-flask") should be(true)
    reqs.requirements.flatMap(_.version).map(_.value).contains("2.10.2") should be(true)
  }

  it should "return tree and spit out unchanged" in {
    val reqs: TreeNode = RequirementsTxtParser.parseFile(simple1)
    reqs.value.trim should equal(simple1.trim)
  }

  it should "allow update in place" in {
    val reqs = RequirementsTxtParser.parseFile(simple1)
    val req0 = reqs.requirements.head
    req0.packageName.value should be("flask")
    req0.version.get.value should be("0.11.1")
    req0.version.get.update("0.12")
    reqs.value.trim should equal(simple1.trim.replace("0.11.1", "0.12"))
  }

  it should "perform upgrade operation" in {
    val reqs = RequirementsTxtParser.parseFile(simple1)
    reqs.update("flask", "0.12")
    reqs.value.trim should equal(simple1.trim.replace("0.11.1", "0.12"))
  }

  it should "add requirement" in {
    val reqs = RequirementsTxtParser.parseFile(simple1)
    val newLine = "the_new_thing==0.12"
    reqs.add(newLine)
    reqs.value.trim.contains(newLine) should be(true)
    reqs.requirements.exists(_.packageName.value.equals("the_new_thing")) should be(true)
  }


  it should "parse without versions" in {
    val reqs = parsesOk(noVersions)
    reqs.requirements.size should be(3)
  }

  it should "parse with versions" in {
    val reqs = parsesOk(withVersions)
    reqs.requirements.size should be(4)
  }

  private def parsesOk(content: String): Requirements = {
    val reqs = RequirementsTxtParser.parseFile(content)
    reqs
  }

}
