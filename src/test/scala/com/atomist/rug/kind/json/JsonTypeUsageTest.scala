package com.atomist.rug.kind.json

import com.atomist.param.SimpleParameterValues
import com.atomist.project.edit.SuccessfulModification
import com.atomist.rug._
import com.atomist.source.{SimpleFileBasedArtifactSource, StringFileArtifact}
import org.scalatest.{FlatSpec, Matchers}

object JsonTypeUsageTest {

  val packageJson =
    """
      |{
      |  "name": "module-name",
      |  "version": "10.3.1",
      |  "description": "An example module to illustrate the usage of a package.json",
      |  "author": "Your Name <you.name@example.org>",
      |  "contributors": [{
      |  "name": "Foo Bar",
      |  "email": "foo.bar@example.com"
      |}],
      |  "bin": {
      |  "module-name": "./bin/module-name"
      |   },
      |  "scripts": {
      |    "test": "vows --spec --isolate",
      |    "start": "node index.js",
      |    "predeploy": "echo im about to deploy",
      |    "postdeploy": "echo ive deployed",
      |    "prepublish": "coffee --bare --compile --output lib/foo src/foo/*.coffee"
      |  },
      |  "main": "lib/foo.js",
      |  "repository": {
      |  "type": "git",
      |  "url": "https://github.com/nodejitsu/browsenpm.org"
      |},
      |  "bugs": {
      |  "url": "https://github.com/nodejitsu/browsenpm.org/issues"
      |},
      |  "keywords": [
      |  "nodejitsu",
      |  "example",
      |  "browsenpm"
      |],
      |  "dependencies": {
      |    "primus": "*",
      |    "async": "~0.8.0",
      |    "express": "4.2.x",
      |    "winston": "git://github.com/flatiron/winston#master",
      |    "bigpipe": "bigpipe/pagelet",
      |    "plates": "https://github.com/flatiron/plates/tarball/master"
      |  },
      |  "devDependencies": {
      |    "vows": "^0.7.0",
      |    "assume": "<1.0.0 || >=2.3.1 <2.4.5 || >=2.5.2 <3.0.0",
      |    "pre-commit": "*"
      |  },
      |  "preferGlobal": true,
      |  "private": true,
      |  "publishConfig": {
      |  "registry": "https://your-private-hosted-npm.registry.nodejitsu.com"
      |},
      |  "subdomain": "foobar",
      |  "analyze": true,
      |  "license": "MIT"
      |}
    """.stripMargin

}

class JsonTypeUsageTest extends FlatSpec with Matchers {

  import JsonTypeUsageTest._

  "JSON type" should "update node value, going via file and path expression" in {
    val edited = updateWith("Rename.ts")
    edited should equal(packageJson.replace("foobar", "absquatulate"))
  }

  it should "update node value, going via file and with" in {
    val edited = updateWith("Rename3.ts")
    edited should equal(packageJson.replace("foobar", "absquatulate"))
  }

  it should "add dependency using Rug" in {
    val edited = updateWith("Rename2.ts")
    // edited should equal(packageJson.replace("foobar", "absquatulate"))
  }

  it should "add dependency using TypeScript" in {
    val edited = updateWith("PackageFinder.ts")
  }

  // Return new content
  private def updateWith(tsFile: String): String = {
    val filepath = "package.json"
    val as = new SimpleFileBasedArtifactSource("as",
      Seq(
        StringFileArtifact(filepath, packageJson)
      )
    )
    val newName = "Foo"

    val ed = TestUtils.editorInSideFile(this, tsFile)

    ed.modify(as,
      SimpleParameterValues(Map(
        "new_name" -> newName
      ))) match {
      case sm: SuccessfulModification =>
        val f = sm.result.findFile(filepath).get
        f.content.contains(s"$newName") should be(true)
        f.content
      case x => fail
    }
  }
}
