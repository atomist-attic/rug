package com.atomist.rug.kind.java

import com.atomist.param.SimpleParameterValues
import com.atomist.rug.TestUtils._
import com.atomist.rug.kind.grammar.AbstractTypeUnderFileTest
import com.atomist.tree.content.text.LineInputPositionImpl

class JavaFileTypeUsageTest extends AbstractTypeUnderFileTest {

  import JavaFileTypeTest._

  override val typeBeingTested = new JavaFileType

  it should "find catch(throwable) and validate format info" in {
    val exceptionToSearchFor = "ThePlaneHasFlownIntoTheMountain"
    val rr = reviewerInSideFile(this, "CatchThrowable.ts").review(ExceptionsSources,
      SimpleParameterValues(Map("exception" -> exceptionToSearchFor)))
    rr.comments.nonEmpty should be (true)
    assert(rr.comments.size === 1)
    val c1 = rr.comments.head
    c1.line should be (defined)
    c1.column should be (defined)
    c1.fileName.get should be (Exceptions.path)

    val pos = LineInputPositionImpl(Exceptions.content, c1.line.get, c1.column.get)
    val extracted = Exceptions.content.substring(pos.offset, pos.offset + exceptionToSearchFor.length)
    assert(extracted === exceptionToSearchFor)
  }

}
