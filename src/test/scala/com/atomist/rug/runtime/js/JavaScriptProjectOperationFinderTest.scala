package com.atomist.rug.runtime.js

import com.atomist.param.SimpleParameterValues
import com.atomist.project.edit.ProjectEditor
import com.atomist.rug.SimpleJavaScriptProjectOperationFinder
import com.atomist.rug.ts.TypeScriptBuilder
import com.atomist.source.{FileArtifact, SimpleFileBasedArtifactSource, StringFileArtifact}
import com.atomist.util.Timing._
import org.scalatest.{FlatSpec, Matchers}

class JavaScriptProjectOperationFinderTest  extends FlatSpec with Matchers {

  val SimpleProjectEditorWithParametersArray: String =
    s"""
       |import {Project} from '@atomist/rug/model/Core'
       |import {File} from '@atomist/rug/model/Core'
       |import {Parameter, Editor} from '@atomist/rug/operations/Decorators'
       |
       |@Editor("Simple", "A nice little editor")
       |class SimpleEditor{
       |
       |    @Parameter({pattern: "^.*$$", description: "foo bar"})
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
       |import {Parameter, Editor} from '@atomist/rug/operations/Decorators'
       |
       |@Editor("Simple", "A nice little editor")
       |class SimpleEditor {
       |
       |    @Parameter({pattern: "^.*$$", description: "foo bar"})
       |    content: string = "Test String";
       |
       |    @Parameter({pattern: "^[0-9]+$$", description: "A nice round number"})
       |    amount: number = 10;
       |
       |    @Parameter({pattern: "^.*$$", description: "A nice round number", required: false})
       |    nope: boolean
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
    assert(eds.parameters.size === 1)
  }

  it should "find an editor using annotated parameters" in {
    val eds = invokeAndVerifySimple(StringFileArtifact(s".atomist/editors/SimpleEditor.ts", SimpleProjectEditorWithAnnotatedParameters))
    assert(eds.parameters.size === 3)
    assert(eds.parameters(0).getDefaultValue === "Test String")
    assert(eds.parameters(1).getDefaultValue === "10")
    assert(eds.parameters(2).getDefaultValue === "")

  }

  it should "run fast, especially when run a whole bunch of times" in {
    //runPerfTest()
  }

  private  def runPerfTest(): Unit = {
    val target = SimpleFileBasedArtifactSource(StringFileArtifact("pom.xml", "nasty stuff"))
    val (as, compileTime) = time {
      val tsf = StringFileArtifact(s".atomist/editors/SimpleEditor.ts", SimpleProjectEditorWithAnnotatedParameters)
      TypeScriptBuilder.compileWithModel(SimpleFileBasedArtifactSource(tsf))
    }
    println(s"Compiling took: $compileTime ms")

    val (ed, evalTime) = time {
      SimpleJavaScriptProjectOperationFinder.find(as).editors.head
    }
    println(s"Loading editor took: $evalTime ms")

    val (_, run1) = time {
      1 to 2 foreach { _ => ed.modify(target,SimpleParameterValues( Map("content" -> "woot")))}
    }
    println(s"1 run took: -> $run1 ms")

    val (_, run10) = time {
      1 to 10 foreach { _ => ed.modify(target,SimpleParameterValues( Map("content" -> "woot")))}
    }
    println(s"10 runs took: -> ${run10/10d} ms/run")

    val (_, run100) = time {
      1 to 100 foreach { _ => ed.modify(target,SimpleParameterValues( Map("content" -> "woot")))}
    }
    println(s"100 runs took: -> ${run100/100d} ms/run")

    val (_, run1000) = time {
      1 to 1000 foreach { _ => ed.modify(target,SimpleParameterValues( Map("content" -> "woot")))}
    }
    println(s"1000 runs took: -> ${run1000/1000d} ms/run")

    val (_, run100000) = time {
      1 to 100000 foreach { _ => ed.modify(target,SimpleParameterValues( Map("content" -> "woot")))}
    }
    println(s"100000 runs took: -> ${run100000/100000d} ms/run")

    val (_, run1000000) = time {
      1 to 1000000 foreach { _ => ed.modify(target,SimpleParameterValues( Map("content" -> "woot")))}
    }
    println(s"1000000 runs took: -> ${run1000000/1000000d} ms/run")
  }

  private  def invokeAndVerifySimple(tsf: FileArtifact): ProjectEditor = {
    val as = TypeScriptBuilder.compileWithModel(SimpleFileBasedArtifactSource(tsf))
    val jsed = SimpleJavaScriptProjectOperationFinder.find(as).editors.head
    assert(jsed.name === "Simple")
    val target = SimpleFileBasedArtifactSource(StringFileArtifact("pom.xml", "nasty stuff"))
    jsed.modify(target,SimpleParameterValues( Map("content" -> "woot")))
    jsed
  }
}
