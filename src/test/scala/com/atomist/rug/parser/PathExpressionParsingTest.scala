package com.atomist.rug.parser

import com.atomist.project.archive.DefaultAtomistConfig
import com.atomist.util.scalaparsing.PathExpressionValue
import com.atomist.source.StringFileArtifact
import org.scalatest.{FlatSpec, Matchers}

/**
  * Created by rod on 10/31/16.
  */
class PathExpressionParsingTest extends FlatSpec with Matchers {

  val atomistConfig = DefaultAtomistConfig

  it should "parse nodes expression" in {
    val prog =
      """
        |editor First
        |
        |let f = $(/src//*[name='application.properties'])
        |
        |with f
        |  do eval { print(f.name() }
      """.stripMargin
    val f = StringFileArtifact(atomistConfig.editorsRoot + "/Redeploy.rug", prog)
    val ed = new ParserCombinatorRugParser().parse(f)
    ed.head.computations.size should be (1)
    ed.head.computations.head.te match {
      case pev: PathExpressionValue =>
    }
  }

  it should "parse scalar expression" in {
    val prog =
      """
        |editor First
        |
        |let f = $(/src//*[name='application.properties']).name
        |
        |with File f
        |  do eval { print(f }
      """.stripMargin
    val f = StringFileArtifact(atomistConfig.editorsRoot + "/Redeploy.rug", prog)
    val ed = new ParserCombinatorRugParser().parse(f)
    ed.head.computations.size should be (1)
    ed.head.computations.head.te match {
      case pev: PathExpressionValue =>
        pev.scalarProperty should equal (Some("name"))
    }
  }
}
