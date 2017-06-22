package com.atomist.rug.kind.java

import com.atomist.parse.java.ParsingTargets
import com.atomist.source.StringFileArtifact
import org.scalatest.{FlatSpec, Matchers}

class JavaSourceMutableViewTest extends FlatSpec with Matchers {

  it should "detect well-formed files" in {
    ParsingTargets.SpringIoGuidesRestServiceSource
      .allFiles
      .filter(f => f.name.endsWith(".java"))
      .map(f => new JavaSourceMutableView(f, null))
      .foreach(f => assert(f.isWellFormed === true))
  }

  it should "detect ill-formed files" in {
    val as = ParsingTargets.SpringIoGuidesRestServiceSource + StringFileArtifact("src/main/java/Rubbish.java", "what in god's holy name are you blathering about")
    as.allFiles
      .filter(f => f.name.endsWith(".java"))
      .map(f => new JavaSourceMutableView(f, null))
      .forall(f => f.isWellFormed) shouldBe false
  }

}
