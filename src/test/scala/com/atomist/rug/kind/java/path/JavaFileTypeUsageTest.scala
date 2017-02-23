package com.atomist.rug.kind.java.path

import com.atomist.param.SimpleParameterValues
import com.atomist.rug.TestUtils._
import com.atomist.rug.kind.grammar.AbstractTypeUnderFileTest

class JavaFileTypeUsageTest extends AbstractTypeUnderFileTest {

  import JavaFileTypeTest._

  override val typeBeingTested = new JavaFileType

  it should "find catch(throwable)" in {

    val rr = reviewerInSideFile(this, "CatchThrowable.ts").review(ExceptionsSources, SimpleParameterValues.Empty)
    rr.comments.nonEmpty should be (true)
    assert(rr.comments.size === 1)
    val c1 = rr.comments.head
    c1.line should be (defined)
    c1.column should be (defined)
    c1.fileName.get should be (Exceptions.path)
    //println(c1)
  }

}
