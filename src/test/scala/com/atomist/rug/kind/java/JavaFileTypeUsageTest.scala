package com.atomist.rug.kind.java

import com.atomist.parse.java.ParsingTargets
import com.atomist.rug.TestUtils._
import com.atomist.rug.kind.grammar.AbstractTypeUnderFileTest
import com.atomist.tree.content.text.LineInputPositionImpl

class JavaFileTypeUsageTest extends AbstractTypeUnderFileTest {

  import JavaFileTypeTest._

  override val typeBeingTested = new JavaFileType

  // TODO we need to re-enable this as a handler test
  it should "find catch(throwable) and validate format info" in pending //{
  //    val exceptionToSearchFor = "ThePlaneHasFlownIntoTheMountain"
  //    val rr = reviewerInSideFile(this, "CatchThrowable.ts").review(ExceptionsSources,
  //      SimpleParameterValues(Map("exception" -> exceptionToSearchFor)))
  //    rr.comments.nonEmpty should be (true)
  //    assert(rr.comments.size === 1)
  //    val c1 = rr.comments.head
  //    c1.line should be (defined)
  //    c1.column should be (defined)
  //    c1.fileName.get should be (Exceptions.path)
  //
  //    val pos = LineInputPositionImpl(Exceptions.content, c1.line.get, c1.column.get)
  //    val extracted = Exceptions.content.substring(pos.offset, pos.offset + exceptionToSearchFor.length)
  //    assert(extracted === exceptionToSearchFor)

  it should "compare methods" in {
    editorInSideFile(this, "CompareMethods.ts").modify(ParsingTargets.SpringIoGuidesRestServiceSource)
  }
}
