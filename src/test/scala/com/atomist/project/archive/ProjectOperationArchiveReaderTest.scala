package com.atomist.project.archive

import com.atomist.project.SimpleProjectOperationArguments
import com.atomist.rug.{Import, TestUtils}
import com.atomist.rug.exec.FakeServiceSource
import com.atomist.rug.runtime.js.TypeScriptRugEditorTest
import com.atomist.rug.runtime.js.interop.NamedJavaScriptEventHandlerTest
import com.atomist.rug.runtime.lang.js.NashornConstructorTest
import com.atomist.source.{SimpleDirectoryArtifact, SimpleFileBasedArtifactSource, StringFileArtifact}
import org.scalatest.{FlatSpec, Matchers}

class ProjectOperationArchiveReaderTest extends FlatSpec with Matchers {

  val atomistConfig: AtomistConfig = DefaultAtomistConfig

  val FirstEditor = StringFileArtifact(atomistConfig.editorsRoot + "/First.rug",
    """
      |editor First
      |
      |with File f do setPath ""
    """.stripMargin
  )

  val SecondOp = StringFileArtifact(atomistConfig.reviewersRoot + "/Second.rug",
    """
      |reviewer Second
      |
      |with File f do setPath ""
    """.stripMargin
  )

  val Generator = StringFileArtifact(atomistConfig.editorsRoot + "/Published.rug",
    """
      |@generator
      |editor Published
      |
      |First
    """.stripMargin
  )

  val EditorWithImports = StringFileArtifact(atomistConfig.editorsRoot + "/EditorWithImports.rug",
    """
      |editor EditorWithImports
      |
      |uses atomist.common-editors.UpdateReadme
      |uses atomist.common-editors.PomParameterizer
      |
      |with File f do setPath ""
    """.stripMargin
  )

  it should "parse single editor" in {
    val apc = new ProjectOperationArchiveReader(atomistConfig)
    val ops = apc.findOperations(new SimpleFileBasedArtifactSource("", FirstEditor), None, Nil)
    ops.editors.size should be(1)
    ops.editorNames should equal(Seq("First"))
    ops.reviewers should equal(Nil)
    ops.reviewerNames should equal(Nil)
    ops.generators should equal(Nil)
    ops.generatorNames should equal(Nil)
  }

  it should "parse editor and reviewer" in {
    val apc = new ProjectOperationArchiveReader(atomistConfig)
    val ops = apc.findOperations(new SimpleFileBasedArtifactSource("", Seq(FirstEditor, SecondOp)), None, Nil)
    ops.editors.size should be(1)
    ops.editorNames should equal(Seq("First"))
    ops.reviewers.size should be(1)
    ops.reviewerNames should equal(Seq("Second"))
    ops.generators should equal(Nil)
    ops.generatorNames should equal(Nil)
  }

  it should "parse editor and reviewer and generator" in {
    val apc = new ProjectOperationArchiveReader(atomistConfig)
    val ops = apc.findOperations(new SimpleFileBasedArtifactSource("", Seq(FirstEditor, SecondOp, Generator)), None, Nil)
    ops.editors.size should be(1)
    ops.editorNames.toSet should equal(Set("First"))
    ops.reviewers.size should be(1)
    ops.reviewerNames should equal(Seq("Second"))
    ops.generators.size should equal(1)
    ops.generatorNames should equal(Seq("Published"))
  }

  it should "find imports in a project that uses them" in {
    val apc = new ProjectOperationArchiveReader(atomistConfig)
    val imports = apc.findImports(new SimpleFileBasedArtifactSource("", Seq(FirstEditor, SecondOp, Generator, EditorWithImports)))
    imports should equal(Seq(
      Import("atomist.common-editors.UpdateReadme"),
      Import("atomist.common-editors.PomParameterizer")
    ))
  }

  it should "find no imports in a project that uses none" in {
    val apc = new ProjectOperationArchiveReader(atomistConfig)
    val imports = apc.findImports(new SimpleFileBasedArtifactSource("", Seq(FirstEditor, SecondOp, Generator)))
    imports should equal(Nil)
  }

  it should "find typescript editor" in {
    val apc = new ProjectOperationArchiveReader(atomistConfig)
    val as = TestUtils.compileWithModel(SimpleFileBasedArtifactSource(
      StringFileArtifact(".atomist/editors/SimpleEditor.ts", TypeScriptRugEditorTest.SimpleEditorTaggedAndMeta)
    ))
    val ops = apc.findOperations(as, None, Nil)
    ops.editors.size should be(1)
    ops.editors.head.parameters.size should be(2)
  }

  val SimpleExecutor =
    """
      |import {Executor} from '@atomist/rug/operations/Executor'
      |import {Parameter, Result, Status} from '@atomist/rug/operations/RugOperation'
      |import {Services} from '@atomist/rug/model/Core'
      |
      |class SimpleExecutor implements Executor {
      |    name: string = "SimpleExecutor"
      |    description: string = "A nice little executionist"
      |    execute(services: Services): Result {
      |
      |        if (services.pathExpressionEngine() == null)
      |         throw new Error("Something is horribly wrong")
      |        return new Result(Status.Success,
      |         `We are clever`)
      |    }
      |}
      |var exec = new SimpleExecutor()
    """.stripMargin
  it should "find typescript executor" in {
    val apc = new ProjectOperationArchiveReader(atomistConfig)
    val as = TestUtils.compileWithModel(SimpleFileBasedArtifactSource(
      StringFileArtifact(".atomist/executors/SimpleExecutor.ts", SimpleExecutor)
    ))
    val ops = apc.findOperations(as, None, Nil)
    ops.executors.size should be(1)
    ops.executors.head.parameters.size should be(0)
    val s2 = new FakeServiceSource(Nil)
    ops.executors.head.execute(s2, SimpleProjectOperationArguments.Empty)
  }

  it should "find and invoke typescript generator" in {
    val apc = new ProjectOperationArchiveReader(atomistConfig)
    val f1 = StringFileArtifact("package.json", "{}")
    val f2 = StringFileArtifact("app/Thing.ts", "class Thing {}")
    val rugAs = TestUtils.compileWithModel(SimpleFileBasedArtifactSource(
      StringFileArtifact(".atomist/editors/SimpleGenerator.ts",
        TypeScriptRugEditorTest.SimpleGenerator),
      f1,
      f2
    ))
    val ops = apc.findOperations(rugAs, None, Nil)
    ops.generators.size should be(1)
    ops.generators.head.parameters.size should be(0)
    val result = ops.generators.head.generate(SimpleProjectOperationArguments.Empty)
    // Should preserve content from the backing archive
    result.findFile(f1.path).get.content.equals(f1.content) should be(true)
    result.findFile(f2.path).get.content.equals(f2.content) should be(true)

    // Should contain new content
    result.findFile("src/from/typescript").get.content.contains("Anders") should be(true)
  }

  it should "allow invocation of other operation from TypeScript editor" in pending

  it should "find and invoke plain javascript generators" in {
    val apc = new ProjectOperationArchiveReader(atomistConfig)
    val f1 = StringFileArtifact("package.json", "{}")
    val f2 = StringFileArtifact("app/Thing.js", "var Thing = {};")


    val rugAs = SimpleFileBasedArtifactSource(
      StringFileArtifact(".atomist/editors/SimpleEditor.js",
        NashornConstructorTest.SimpleJavascriptEditor),
      f1,
      f2
    ) + TestUtils.user_model



    val ops = apc.findOperations(rugAs, None, Nil)
    ops.editors.size should be(1)
    ops.editors.head.parameters.size should be(1)
  }

  it should "ignore unbound handler" in {
    val apc = new ProjectOperationArchiveReader(atomistConfig)
    val f1 = StringFileArtifact("package.json", "{}")
    val f2 = StringFileArtifact("app/Thing.ts", "class Thing {}")

    // We don't know the Atomist declared variable. So ignore it.
    val handler =
      s"""
         |import {Atomist} from "@atomist/rug/operations/Handler"
         |import {Project,File} from "@atomist/rug/model/Core"
         |
         |declare var atomist: Atomist  // <= this is for the compiler only
         |
         |declare var print: any
         |
         |atomist.on<Project,File>('/src/main/**.java', m => {
         |   print(`in handler with $${m}`)
         |   print(`Root=$${m.root()}, leaves=$${m.matches()}`)
         |})
         |
      """.stripMargin
    val rugAs = TestUtils.compileWithModel(SimpleFileBasedArtifactSource(
      StringFileArtifact(".atomist/editors/SimpleGenerator.ts",
        TypeScriptRugEditorTest.SimpleGenerator),
      f1,
      f2,
      StringFileArtifact(".atomist/handlers/sub.ts", handler)
    ))
    val ops = apc.findOperations(rugAs, None, Nil)
    ops.generators.size should be(1)
    ops.generators.head.parameters.size should be(0)
    val result = ops.generators.head.generate(SimpleProjectOperationArguments.Empty)
    // Should preserve content from the backing archive
    result.findFile(f1.path).get.content.equals(f1.content) should be(true)
    result.findFile(f2.path).get.content.equals(f2.content) should be(true)

    // Should contain new contain
    result.findFile("src/from/typescript").get.content.contains("Anders") should be(true)
  }

  it should "ignore unbound handler in the new style" in {
    val apc = new ProjectOperationArchiveReader(atomistConfig)
    val f1 = StringFileArtifact("package.json", "{}")
    val f2 = StringFileArtifact("app/Thing.ts", "class Thing {}")


    val rugAs = TestUtils.compileWithModel(SimpleFileBasedArtifactSource(
      StringFileArtifact(".atomist/editors/SimpleGenerator.ts",
        TypeScriptRugEditorTest.SimpleGenerator),
      f1,
      f2,
      NamedJavaScriptEventHandlerTest.reOpenCloseIssueProgram
    ))
    val ops = apc.findOperations(rugAs, None, Nil)
    ops.generators.size should be(1)
    ops.generators.head.parameters.size should be(0)
    val result = ops.generators.head.generate(SimpleProjectOperationArguments.Empty)
    // Should preserve content from the backing archive
    result.findFile(f1.path).get.content.equals(f1.content) should be(true)
    result.findFile(f2.path).get.content.equals(f2.content) should be(true)

    // Should contain new contain
    result.findFile("src/from/typescript").get.content.contains("Anders") should be(true)
  }

}
