package com.atomist.rug.kind.yml

import java.io.StringReader

import com.atomist.project.SimpleProjectOperationArguments
import com.atomist.project.edit.{NoModificationNeeded, SuccessfulModification}
import com.atomist.rug.DefaultRugPipeline
import com.atomist.rug.InterpreterRugPipeline.DefaultRugArchive
import com.atomist.rug.TestUtils.attemptModification
import com.atomist.source._
import org.scalatest.{FlatSpec, Matchers}
import org.yaml.snakeyaml.Yaml
import scala.collection.JavaConverters._

abstract class AbstractYmlUsageTest extends FlatSpec with Matchers {

  private val parser = new Yaml()

  protected def runProgAndCheck(prog: String, as: ArtifactSource, mods: Int): ArtifactSource = {
    val progArtifact: ArtifactSource = new SimpleFileBasedArtifactSource(DefaultRugArchive,
      StringFileArtifact(new DefaultRugPipeline().defaultFilenameFor(prog), prog)
    )

    val modAttempt = attemptModification(progArtifact, as, EmptyArtifactSource(""),
      SimpleProjectOperationArguments("", Map.empty[String, Object]))

    modAttempt match {
      case sm: SuccessfulModification =>
        assert(sm.result.cachedDeltas.size === mods)
        sm.result.cachedDeltas.foreach {
          case fud: FileUpdateDelta =>
//            val yml = fud.updatedFile.content
//            val events = parser.parse(new StringReader(yml))
//            println(s"Parsed ${fud.updatedFile}: ${events.asScala.mkString(",")}")
            // TODO how do we validate YML? SnakeYAML seems to let everything through
          case x => fail(s"Unexpected change: $x")
        }
        sm.result
      case _: NoModificationNeeded if mods > 0 =>
        fail(s"No modification made when $mods expected: $prog; \n${ArtifactSourceUtils.prettyListFiles(as)}")
      case _: NoModificationNeeded if mods == 0 =>
        as
    }
  }

}
