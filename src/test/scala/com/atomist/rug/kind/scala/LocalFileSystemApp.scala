package com.atomist.rug.kind.scala

import java.io.File

import com.atomist.project.SimpleProjectOperationArguments
import com.atomist.project.edit.SuccessfulModification
import com.atomist.rug.TestUtils
import com.atomist.source.FileUpdateDelta
import com.atomist.source.file.{FileSystemArtifactSource, FileSystemArtifactSourceIdentifier}

/**
  * Useful for profiling
  */
object LocalFileSystemApp extends App {

  import com.atomist.util.Timing._

  val as = new FileSystemArtifactSource(FileSystemArtifactSourceIdentifier(new File("/Users/rod/sforzando-dev/idea-projects/rug")))

  val runs = 50

  val pe = TestUtils.editorInSideFile(this, "EqualsToSymbol.ts")

  for (i <- 1 until runs) {

    val (deltas, ms) = time {
      pe.modify(as, SimpleProjectOperationArguments.Empty) match {
        case sm: SuccessfulModification =>
          //new FileSystemArtifactSourceWriter().write(sm.result, )
          val deltas = sm.result.deltaFrom(as)
          deltas.deltas.foreach {
            case fud: FileUpdateDelta =>
            //println(s"${fud.path}\n${fud.updatedFile.content}\n\n")
            case _ => ???
          }
          deltas
        case _ => ???
      }
    }
    println(s"Run $i/${runs} succeeded in ${ms}ms with ${deltas.deltas.size} files updated")


  }

}
