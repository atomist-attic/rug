package com.atomist.rug.kind.yaml

import com.atomist.param.SimpleParameterValues
import com.atomist.project.edit.{NoModificationNeeded, SuccessfulModification}
import com.atomist.rug.TestUtils
import com.atomist.source._
import org.scalatest.{FlatSpec, Matchers}

trait AbstractYamlUsageTest extends FlatSpec with Matchers {

  protected def runProgAndCheck(ts: String, as: ArtifactSource, mods: Int, params: Map[String,String]): ArtifactSource = {
    val pe = TestUtils.editorInSideFile(this, ts)

    pe.modify(as, SimpleParameterValues(params)) match {
      case sm: SuccessfulModification =>
        assert(sm.result.cachedDeltas.size === mods)
        sm.result.cachedDeltas.foreach {
          case fud: FileUpdateDelta =>
          // TODO how do we validate YAML? SnakeYAML seems to let everything through
          case x => fail(s"Unexpected change: $x")
        }
        sm.result
      case _: NoModificationNeeded if mods > 0 =>
        fail(s"No modification made when $mods expected: $ts; \n${ArtifactSourceUtils.prettyListFiles(as)}")
      case _: NoModificationNeeded if mods == 0 =>
        as
    }
  }
}
