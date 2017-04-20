package com.atomist.rug.kind.impact

import com.atomist.source.{SimpleFileBasedArtifactSource, StringFileArtifact}
import org.scalatest.{FlatSpec, Matchers}

class ImpactTest extends FlatSpec with Matchers {

  "Impact" should "allow descemt" in {
    val oldAs = SimpleFileBasedArtifactSource()
    val newAs = oldAs + StringFileArtifact("README.md", "Add stuff to this project")
    val impact = new Impact(null, oldAs, newAs)
    assert(impact.hasTag("Impact"))
  }

}
