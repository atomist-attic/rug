package com.atomist.rug.kind.yml

import com.atomist.project.SimpleProjectOperationArguments
import com.atomist.project.edit.{NoModificationNeeded, SuccessfulModification}
import com.atomist.rug.DefaultRugPipeline
import com.atomist.rug.InterpreterRugPipeline.DefaultRugArchive
import com.atomist.rug.TestUtils.attemptModification
import com.atomist.source._
import org.scalatest.{FlatSpec, Matchers}

// TODO this isn't YML specific so could be pulled into generic infrastructure
abstract class AbstractYmlUsageTest extends FlatSpec with Matchers {

  protected def runProgAndCheck(prog: String, as: ArtifactSource, mods: Int): ArtifactSource = {
    val progArtifact: ArtifactSource = new SimpleFileBasedArtifactSource(DefaultRugArchive,
      StringFileArtifact(new DefaultRugPipeline().defaultFilenameFor(prog), prog)
    )

    val modAttempt = attemptModification(progArtifact, as, EmptyArtifactSource(""),
      SimpleProjectOperationArguments("", Map.empty[String, Object]))

    modAttempt match {
      case sm: SuccessfulModification =>
        assert(sm.result.cachedDeltas.size === mods)
        sm.result
      case _: NoModificationNeeded if mods > 0 =>
        fail(s"No modification made when $mods expected: $prog; \n${ArtifactSourceUtils.prettyListFiles(as)}")
      case _: NoModificationNeeded if mods == 0 =>
        as
    }
  }

}
