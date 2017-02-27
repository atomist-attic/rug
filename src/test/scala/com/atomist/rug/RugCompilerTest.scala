package com.atomist.rug

import com.atomist.source._
import com.typesafe.scalalogging.LazyLogging

object RugCompilerTest extends LazyLogging {

  val JavaAndText: ArtifactSource = new SimpleFileBasedArtifactSource("name",
    Seq(
      StringFileArtifact("pom.xml", "<maven></maven"),
      StringFileArtifact("message.txt", "// I'm talkin' about ethics"),
      StringFileArtifact("/src/main/java/Dog.java",
        """class Dog {}""".stripMargin)
    )
  )

  def show(as: ArtifactSource): Unit = {
    as.allFiles.foreach(f => {
      logger.debug(f.path + "\n" + f.content + "\n\n")
    })
  }
}
