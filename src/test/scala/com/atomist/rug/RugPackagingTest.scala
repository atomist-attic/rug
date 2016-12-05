package com.atomist.rug

import com.atomist.project.archive.{AtomistConfig, DefaultAtomistConfig}
import com.atomist.source.{SimpleFileBasedArtifactSource, StringFileArtifact}
import org.scalatest.{FlatSpec, Matchers}

class RugPackagingTest extends FlatSpec with Matchers {

  val atomistConfig: AtomistConfig = DefaultAtomistConfig

  val fileBase = atomistConfig.editorsRoot + "/"

  val rp = new DefaultRugPipeline()

  it should "compile validly packaged programs" in {
    val as = new SimpleFileBasedArtifactSource("", Seq(
      StringFileArtifact(fileBase + "First.rug", "editor First  Second"),
      StringFileArtifact(fileBase + "Second.rug", "editor Second with file f do setName 'foo'")
    ))
    val ops = rp.create(as, None, Nil)
    ops.size should be(2)
  }

  it should "reject invalidly packaged programs" in {
    val as = new SimpleFileBasedArtifactSource("", Seq(
      StringFileArtifact(fileBase + "Fxirst.rug", "editor First  Second"),
      StringFileArtifact(fileBase + "Second.rug", "editor Second with file f do setName 'foo'")
    ))
    an[BadRugPackagingException] should be thrownBy rp.create(as, None, Nil)
  }

  it should "accept multiple programs in single source file with no external references" in {
    val as = new SimpleFileBasedArtifactSource("", Seq(
      StringFileArtifact(fileBase + "First.rug", "editor First Second\neditor Second with file f do setName 'foo'")
    ))
    rp.create(as, None).size should be (2)
  }

  it should "reject multiple programs in single source file with non-matching name" in {
    val as = new SimpleFileBasedArtifactSource("", Seq(
      StringFileArtifact(fileBase + "X.rug", "editor First Second\neditor Second with file f do setName 'foo'")
    ))
    an[BadRugPackagingException] should be thrownBy rp.create(as, None)
  }

  it should "reject multiple programs in single source file with external references" in {
    val as = new SimpleFileBasedArtifactSource("", Seq(
      StringFileArtifact(fileBase + "First.rug", "editor First  Second\neditor Second with file f do setName 'foo'"),
      StringFileArtifact(fileBase + "Third.rug", "editor Third Second\n")
    ))
    an[BadRugPackagingException] should be thrownBy rp.create(as, None)
  }

  it should "reject editor packaged under reviewers root" in {
    val as = new SimpleFileBasedArtifactSource("", Seq(
      StringFileArtifact(atomistConfig.reviewersRoot + "/First.rug", "editor First with file f do setPath 'foo'")
    ))
    an[BadRugPackagingException] should be thrownBy rp.create(as, None)
  }

}
