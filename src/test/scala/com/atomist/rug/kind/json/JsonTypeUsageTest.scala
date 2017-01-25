package com.atomist.rug.kind.json

import com.atomist.project.SimpleProjectOperationArguments
import com.atomist.rug.InterpreterRugPipeline.DefaultRugArchive
import com.atomist.rug.compiler.typescript.TypeScriptCompiler
import com.atomist.rug.compiler.typescript.compilation.CompilerFactory
import com.atomist.rug.kind.DefaultTypeRegistry
import com.atomist.rug.ts.{RugTranspiler, TypeScriptBuilder}
import com.atomist.rug._
import com.atomist.source.{ArtifactSource, EmptyArtifactSource, SimpleFileBasedArtifactSource, StringFileArtifact}
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
        |let subdomainNode = $(/*[@name='package.json']/Json()/subdomain)
        |
        |with subdomainNode
        | do setValue "absquatulate"
      """.stripMargin
    val edited = updateWith(new SimpleFileBasedArtifactSource(DefaultRugArchive, StringFileArtifact(new DefaultRugPipeline().defaultFilenameFor(prog), prog)))
    edited should equal(packageJson.replace("foobar", "absquatulate"))
  }

  it should "update node value, going via file and with" in {
    val prog =
      """
        |editor Rename
        |
        |with File when path = "package.json"
        | with Json
        |   with subdomain
        |     do setValue "absquatulate"
      """.stripMargin
    val edited = updateWith(new SimpleFileBasedArtifactSource(DefaultRugArchive, StringFileArtifact(new DefaultRugPipeline().defaultFilenameFor(prog), prog))
    )
    edited should equal(packageJson.replace("foobar", "absquatulate"))
  }

  it should "add dependency using Rug" in {
    val prog =
      """
        |editor Rename
        |
        |let dependencies = $(/*[@name='package.json']/Json()/dependencies)
        |
        |with dependencies
        | do addKeyValue "foo" "bar"
      """.stripMargin
    val edited = updateWith(new SimpleFileBasedArtifactSource(DefaultRugArchive, StringFileArtifact(new DefaultRugPipeline().defaultFilenameFor(prog), prog))
    )
    // edited should equal(packageJson.replace("foobar", "absquatulate"))
  }

  it should "add dependency using TypeScript" in {
    val program =
      """
        |import {ProjectEditor} from "@atomist/rug/operations/ProjectEditor"
        |import {Status, Result} from "@atomist/rug/operations/RugOperation"
        |import {Project,Pair} from '@atomist/rug/model/Core'
        |import {Match,PathExpression,PathExpressionEngine,TreeNode} from '@atomist/rug/tree/PathExpression'
        |
        |class PackageFinder implements ProjectEditor {
        |    name: string = "node.deps"
        |    description: string = "Finds package.json dependencies"
        |    edit(project: Project) {
        |
        |      let eng: PathExpressionEngine = project.context().pathExpressionEngine();
        |      let pe = new PathExpression<Project,Pair>(`/*[@name='package.json']/Json()/dependencies`)
        |      let p = eng.scalar(project, pe)
        |      //if (p == null)
        |      p.addKeyValue("foo", "bar")
        |    }
        |}
        |
        |export let finder = new PackageFinder();
      """.stripMargin
    val pas = TypeScriptBuilder.compileWithModel(new SimpleFileBasedArtifactSource(DefaultRugArchive, StringFileArtifact(new DefaultRugPipeline().defaultFilenameFor(program), program)))
    val edited = updateWith(pas, new CompilerChainPipeline(Seq(new TypeScriptCompiler(CompilerFactory.create()))))
  }

  // Return new content
  private def updateWith(prog: ArtifactSource,
                         pipeline: RugPipeline = new DefaultRugPipeline(DefaultTypeRegistry)): String = {
    val filepath = "package.json"
    val as = new SimpleFileBasedArtifactSource("as",
      Seq(
        StringFileArtifact(filepath, packageJson)
      )
    )
    val newName = "Foo"

    val r = doModification(prog, as, EmptyArtifactSource(InterpreterRugPipeline.DefaultRugArchive),
      SimpleProjectOperationArguments("", Map(
      "new_name" -> newName
    )), pipeline = pipeline)

    val f = r.findFile(filepath).get
    f.content.contains(s"$newName") should be(true)
    f.content
  }
}
