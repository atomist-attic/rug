package com.atomist.rug.kind.scala

import com.atomist.project.edit.SuccessfulModification
import com.atomist.rug.kind.DefaultTypeRegistry
import com.atomist.rug.kind.core.ProjectMutableView
import com.atomist.rug.kind.grammar.AbstractTypeUnderFileTest
import com.atomist.source.EmptyArtifactSource
import com.atomist.tree.utils.TreeNodeUtils

class ScalaFileTypeUsageTest extends AbstractTypeUnderFileTest {

  import ScalaFileTypeTest._
  import com.atomist.tree.pathexpression.PathExpressionParser._

  override val typeBeingTested = new ScalaFileType

  it should "change exception catch ???" is pending

  it should "upgrade ScalaTest assertions" in pendingUntilFixed {

    import TreeNodeUtils._

//    val pmv = new ProjectMutableView(EmptyArtifactSource(), ScalaTestSources )
//    expressionEngine.evaluate(pmv, "/src/test/scala//ScalaFile()", DefaultTypeRegistry) match {
//      case Right(nodes) =>
//        println(toShorterString(nodes.head, NameAndContentStringifier))
//      case _ => ???
//    }

    modify("UpgradeScalaTestAssertions.ts", ScalaTestSources) match {
      case sm: SuccessfulModification =>
        //val theFile = sm.result.findFile("src/exception.cs").get
        //theFile.content should be (Exceptions.replace("IndexOutOfRangeException", newExceptionType))
      case wtf => fail(s"Expected SuccessfulModification, not $wtf")
    }
  }

}

