package com.atomist.rug.runtime.js

import com.atomist.project.SimpleProjectOperationArguments
import com.atomist.rug.ts.TypeScriptBuilder
import com.atomist.source.{FileArtifact, SimpleFileBasedArtifactSource, StringFileArtifact}
import org.scalatest.{FlatSpec, Matchers}
import com.atomist.util.Timing._


class JavaScriptOperationFinderTest  extends FlatSpec with Matchers {

  val SimpleProjectEditorWithParametersArray: String =
    s"""
       |import {Project} from '@atomist/rug/model/Core'
       |import {File} from '@atomist/rug/model/Core'
       |import {parameter, editor} from '@atomist/rug/operations/RugOperation'
       |
       |@editor("Simple", "A nice little editor")
       |class SimpleEditor{
       |
       |    @parameter({pattern: "^.*$$", description: "foo bar"})
       |    content: string
       |
       |    edit(project: Project) {}
       |  }
       |export let myeditor = new SimpleEditor()
    """.stripMargin

  val SimpleProjectEditorWithAnnotatedParameters: String =
    s"""
       |import {Project} from '@atomist/rug/model/Core'
       |import {File} from '@atomist/rug/model/Core'
       |import {parameter, editor} from '@atomist/rug/operations/RugOperation'
       |
       |@editor("Simple", "A nice little editor")
       |class SimpleEditor {
       |
       |    @parameter({pattern: "^.*$$", description: "foo bar"})
       |    content: string = "Test String";
       |
       |    @parameter({pattern: "^\\d+$$", description: "A nice round number"})
       |    amount: number = 10;
       |
       |    @parameter({pattern: "^\\d+$$", description: "A nice round number"})
       |    nope: boolean;
       |
       |    edit(project: Project) {
       |       if(this.amount != 10) {
       |          throw new Error("Number should be 10!");
       |       }
       |       if(this.content != "woot") {
       |          throw new Error("Name should be woot");
       |       }
       |    }
       |  }
       |export let myeditor = new SimpleEditor()
    """.stripMargin

  it should "find an editor with a parameters list" in {
    val eds = invokeAndVerifySimple(StringFileArtifact(s".atomist/editors/SimpleEditor.ts", SimpleProjectEditorWithParametersArray))
    eds.parameters.size should be(1)
  }

  it should "find an editor using annotated parameters" in {
    val eds = invokeAndVerifySimple(StringFileArtifact(s".atomist/editors/SimpleEditor.ts", SimpleProjectEditorWithAnnotatedParameters))
    eds.parameters.size should be(3)
    eds.parameters(0).getDefaultValue should be("Test String")
    eds.parameters(1).getDefaultValue should be("10")
    eds.parameters(2).getDefaultValue should be("")

  }

  it should "run fast, especially when run a whole bunch of times" in {
    //runPerfTest()
  }

  private def runPerfTest(): Unit = {
    val target = SimpleFileBasedArtifactSource(StringFileArtifact("pom.xml", "nasty stuff"))
    val (as, compileTime) = time {
      val tsf = StringFileArtifact(s".atomist/editors/SimpleEditor.ts", SimpleProjectEditorWithAnnotatedParameters)
      TypeScriptBuilder.compileWithModel(SimpleFileBasedArtifactSource(tsf))
    }
    println(s"Compiling took: $compileTime ms")

    val (ed, evalTime) = time {
      JavaScriptOperationFinder.fromJavaScriptArchive(as).head.asInstanceOf[JavaScriptInvokingProjectEditor]
    }
    println(s"Loading editor took: $evalTime ms")

    val (_, run1) = time {
      1 to 2 foreach { _ => ed.modify(target,SimpleProjectOperationArguments("", Map("content" -> "woot")))}
    }
    println(s"1 run took: -> $run1 ms")

    val (_, run10) = time {
      1 to 10 foreach { _ => ed.modify(target,SimpleProjectOperationArguments("", Map("content" -> "woot")))}
    }
    println(s"10 runs took: -> ${run10/10d} ms/run")

    val (_, run100) = time {
      1 to 100 foreach { _ => ed.modify(target,SimpleProjectOperationArguments("", Map("content" -> "woot")))}
    }
    println(s"100 runs took: -> ${run100/100d} ms/run")

    val (_, run1000) = time {
      1 to 1000 foreach { _ => ed.modify(target,SimpleProjectOperationArguments("", Map("content" -> "woot")))}
    }
    println(s"1000 runs took: -> ${run1000/1000d} ms/run")

    val (_, run100000) = time {
      1 to 100000 foreach { _ => ed.modify(target,SimpleProjectOperationArguments("", Map("content" -> "woot")))}
    }
    println(s"100000 runs took: -> ${run100000/100000d} ms/run")

    val (_, run1000000) = time {
      1 to 1000000 foreach { _ => ed.modify(target,SimpleProjectOperationArguments("", Map("content" -> "woot")))}
    }
    println(s"1000000 runs took: -> ${run1000000/1000000d} ms/run")
  }


  private def invokeAndVerifySimple(tsf: FileArtifact): JavaScriptInvokingProjectEditor = {
    val as = TypeScriptBuilder.compileWithModel(SimpleFileBasedArtifactSource(tsf))
    val jsed = JavaScriptOperationFinder.fromJavaScriptArchive(as).head.asInstanceOf[JavaScriptInvokingProjectEditor]
    jsed.name should be("Simple")
    val target = SimpleFileBasedArtifactSource(StringFileArtifact("pom.xml", "nasty stuff"))
    jsed.modify(target,SimpleProjectOperationArguments("", Map("content" -> "woot")))
    jsed
  }
}
