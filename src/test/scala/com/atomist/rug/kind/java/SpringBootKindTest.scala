package com.atomist.rug.kind.java

import org.scalatest.{FlatSpec, Matchers}
import JavaVerifier._
import com.atomist.param.SimpleParameterValues
import com.atomist.project.edit.SuccessfulModification
import com.atomist.rug.TestUtils

class SpringBootKindTest extends FlatSpec with Matchers {

  import JavaTypeUsageTest._

  it should "annotate field" in {
    val ed = TestUtils.editorInSideFile(this, "ClassAnnotated.ts")
    ed.modify(NewSpringBootProject, SimpleParameterValues.Empty) match {
      case sm: SuccessfulModification =>
        val appClass = sm.result.findFile("src/main/java/com/atomist/test1/Test1Application.java").get
        appClass.content.contains("@Foobar") should be(true)
        appClass.content.contains("import com.someone.Foobar") should be(true)
        verifyJavaIsWellFormed(sm.result)
      case x => fail(s"Unexpcted result: $x")
    }
  }

}
