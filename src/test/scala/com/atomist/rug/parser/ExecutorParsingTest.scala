package com.atomist.rug.parser

import com.atomist.project.archive.DefaultAtomistConfig
import com.atomist.source.StringFileArtifact
import org.scalatest.{FlatSpec, Matchers}

class ExecutorParsingTest extends FlatSpec with Matchers {

  val atomistConfig = DefaultAtomistConfig

  it should "find single file" in {
    val prog =
      """
        |executor First
        |
        |with Project f
        | when { f.name().contains("k8") }
        |   editWith Foobar
      """.stripMargin
    val f = StringFileArtifact(atomistConfig.editorsRoot + "/Redeploy.rug", prog)
    new ParserCombinatorRugParser().parse(f)
  }
}