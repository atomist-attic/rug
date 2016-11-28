package com.atomist.rug.kind.java

import com.atomist.rug.kind.java.JavaClassType._
import com.atomist.source.ArtifactSource
import com.github.javaparser.JavaParser
import org.scalatest.Matchers

/**
  * Utilities for use in testing.
  */
object JavaVerifier extends Matchers {

  /**
    * Verify that the contents of this artifact source are still well formed
    */
  def verifyJavaIsWellFormed(result: ArtifactSource): Unit = {
    for {
      f <- result.allFiles
      if f.name.endsWith(JavaExtension)
    } {
      try {
        JavaParser.parse(f.inputStream)
      }
      catch {
        case t: Throwable => fail(s"File ${f.path} is ill-formed\n${f.content}", t)
      }
    }
  }
}
