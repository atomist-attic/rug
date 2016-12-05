package com.atomist.rug.kind.json

import com.atomist.project.SimpleProjectOperationArguments
import com.atomist.rug.kind.DefaultTypeRegistry
import com.atomist.rug.ts.RugTranspiler
import com.atomist.rug.{CompilerChainPipeline, DefaultRugPipeline, RugPipeline}
import com.atomist.source.{EmptyArtifactSource, SimpleFileBasedArtifactSource, StringFileArtifact}
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
      |},
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
  import com.atomist.rug.TestUtils._

  private val ccPipeline = new CompilerChainPipeline(Seq(new RugTranspiler()))

  it should "update node value, going via file and path expression" in {
    val prog =
      """
        |editor Rename
        |
        |let subdomainNode = $(/[name='package.json']->json/subdomain)
        |
        |with subdomainNode
        | do setValue "absquatulate"
      """.stripMargin
    val edited = updateWith(prog)
    edited should equal(packageJson.replace("foobar", "absquatulate"))
  }

  it should "update node value, going via file and with" in {
    val prog =
      """
        |editor Rename
        |
        |with file when path = "package.json"
        | with json
        |   with subdomain
        |     do setValue "absquatulate"
      """.stripMargin
    val edited = updateWith(prog)
    edited should equal(packageJson.replace("foobar", "absquatulate"))
  }

  it should "add dependency using Rug" in {
    val prog =
      """
        |editor Rename
        |
        |let dependencies = $(/[name='package.json']->json/dependencies)
        |
        |with dependencies
        | do addKeyValue "foo" "bar"
      """.stripMargin
    val edited = updateWith(prog)
    // edited should equal(packageJson.replace("foobar", "absquatulate"))
  }

  it should "add dependency using TypeScript" in {
    val program =
      """
        |import {ParameterlessProjectEditor} from "user-model/operations/ProjectEditor"
        |import {Status, Result} from "user-model/operations/Result"
        |import {Project,Pair} from 'user-model/model/Core'
        |import {Match,PathExpression,PathExpressionEngine,TreeNode} from 'user-model/tree/PathExpression'
        |
        |import {editor, inject} from '@atomist/rug/support/Metadata'
        |
        |declare var print
        |
        |@editor("Dependency adder")
        |class PackageFinder extends ParameterlessProjectEditor {
        |
        | private eng: PathExpressionEngine;
        |
        |    constructor(@inject("PathExpressionEngine") _eng: PathExpressionEngine ){
        |      super();
        |      this.eng = _eng;
        |    }
        |
        |    editWithoutParameters(project: Project): Result {
        |
        |    let pe = new PathExpression<Project,Pair>(`/[name='package.json']->json/dependencies`)
        |    let p = this.eng.scalar(project, pe)
        |    //if (p == null)
        |    p.addKeyValue("foo", "bar")
        |    return new Result(Status.Success, "OK");
        |    }
        |}
      """.stripMargin
    val edited = updateWith(program, ccPipeline)
  }

  // Return new content
  private def updateWith(prog: String,
                         pipeline: RugPipeline = new DefaultRugPipeline(DefaultTypeRegistry)): String = {
    val filepath = "package.json"
    val as = new SimpleFileBasedArtifactSource("as",
      Seq(
        StringFileArtifact(filepath, packageJson)
      )
    )
    val newName = "Foo"
    val r = doModification(prog, as, EmptyArtifactSource(""),
      SimpleProjectOperationArguments("", Map(
      "new_name" -> newName
    )), pipeline = pipeline)

    val f = r.findFile(filepath).get
    f.content.contains(s"$newName") should be(true)
    f.content
  }
}
