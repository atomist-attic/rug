package com.atomist.rug.kind.rug

import com.atomist.rug.kind.grammar.{AntlrRawFileType, AbstractTypeUnderFileTest}
import com.atomist.rug.kind.rug.dsl.RugFileType
import com.atomist.project.edit.{NoModificationNeeded, SuccessfulModification}

class RugFileTypeUsageTest extends AbstractTypeUnderFileTest {

  override protected  def typeBeingTested: AntlrRawFileType = new RugFileType


  import RugFileTypeTest._

  it should "enumerate editors in a Rug DSL" in {
    val r = modify("ListEditors.ts", TwoEditors)
    r match {
      case nmn: NoModificationNeeded =>
      
      case _ => ???
    }
  }

  it should "enumerate parameters of each editor in a Rug DSL" in {
    val r = modify("ListParams.ts", ManyParamsEditor)
    r match {
      case nmn: NoModificationNeeded =>
      
      case _ => ???
    }
  }

  it should "enumerate used editors of each editor in a Rug DSL" in {
    val r = modify("ListUses.ts", UsesVariousEditor)
    r match {
      case nmn: NoModificationNeeded =>
      
      case _ => ???
    }
  }

  it should "enumerate editors relying semver in a Rug DSL" in {
    val r = modify("ListEditorsUsingSemanticVersion.ts", SomeUsingSemverEditor)
    r match {
      case nmn: NoModificationNeeded =>
      
      case _ => ???
    }
  }

  it should "remove old @generator annotation and replace editor with generator" in {
    val r = modify("ReplaceEditorWithGenerator.ts", UsingOldGeneratorAnnotationEditor)
    r match {
      case nmn: NoModificationNeeded =>
      
      case sma: SuccessfulModification =>
        val rugs = sma.result.files.filter(_.name.endsWith(".rug"))
        assert(rugs.size === 1)
        rugs.foreach(f => {
          f.content contains "@generator \"UberGenerator\"" should be(false)
          f.content contains "editor UberGenerator" should be(false)
          f.content contains "generator UberGenerator" should be(true)
        })
      case _ => ???
    }
  }
}
