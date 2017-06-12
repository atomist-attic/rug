package com.atomist.rug.runtime.js.nashorn

import com.atomist.rug.runtime.js.{JavaScriptEngineContextFactory, JavaScriptProjectEditor}
import com.atomist.rug.ts.TypeScriptBuilder
import com.atomist.rug.{RugArchiveReader, RugJavaScriptException}
import com.atomist.source.{SimpleFileBasedArtifactSource, StringFileArtifact}
import org.scalatest.{FlatSpec, Matchers}

class NashornContextTest extends FlatSpec with Matchers {
  val SimpleEditorInvokingOtherEditorAndAddingToOurOwnParameters: String =
    s"""
       |import {Project} from '@atomist/rug/model/Core'
       |import {Editor, Parameter} from '@atomist/rug/operations/Decorators'
       |import {File} from '@atomist/rug/model/Core'
       |
       |@Editor("Simple","A nice little editor")
       |class SimpleEditor  {
       |    @Parameter({description: "Content", pattern: "@url", maxLength: 100})
       |    content: string = "http://t.co"
       |
       |    edit(project: Project) {
       |    }
       |  }
       |export let editor = new SimpleEditor()
    """.stripMargin

  val SimpleEditorWithoutParameters: String =
    """
      |guff js?
    """.stripMargin

  it should "throw an exception containing the JS file name if there are error during eval" in {
    val filename = ".atomist/editors/SimpleEditor.js"
    val caught = intercept[RugJavaScriptException] {
      JavaScriptEngineContextFactory.create(SimpleFileBasedArtifactSource(StringFileArtifact(filename, SimpleEditorWithoutParameters)))
    }
    caught.getMessage should include(filename)
  }

  it should "create two separate js objects for each operation" in {
    val tsf = StringFileArtifact(s".atomist/editors/SimpleEditor.ts", SimpleEditorInvokingOtherEditorAndAddingToOurOwnParameters)
    val as = TypeScriptBuilder.compileWithModel(SimpleFileBasedArtifactSource(tsf))
    val jsed = RugArchiveReader(as).editors.head.asInstanceOf[JavaScriptProjectEditor]
    val v1 = jsed.jsc.asInstanceOf[NashornContext].cloneVar(jsed.jsVar.asInstanceOf[NashornJavaScriptObject].som)
    v1.setMember("__description", "dude")
    jsed.jsVar.asInstanceOf[NashornJavaScriptObject].som.get("__description") should be ("A nice little editor")
  }
}
