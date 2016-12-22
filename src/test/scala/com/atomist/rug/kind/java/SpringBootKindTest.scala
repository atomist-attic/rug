package com.atomist.rug.kind.java

import org.scalatest.{FlatSpec, Matchers}

import JavaVerifier._

class SpringBootKindTest extends FlatSpec with Matchers {

  import JavaTypeUsageTest._

  it should "annotate field" in {
    val program =
      """
        |@description "I add Foobar annotations"
        |editor ClassAnnotated
        |
        |with SpringBootProject p
        |do
        |  annotateBootApplication "com.someone" "Foobar"
      """.stripMargin
    val r = executeJava(program, "editors/ClassAnnotated.rug",NewSpringBootProject)
    val appClass = r.findFile("src/main/java/com/atomist/test1/Test1Application.java").get
    appClass.content.contains("@Foobar") should be(true)
    appClass.content.contains("import com.someone.Foobar") should be(true)
    verifyJavaIsWellFormed(r)
  }

}
