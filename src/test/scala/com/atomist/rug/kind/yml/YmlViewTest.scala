package com.atomist.rug.kind.yml

import com.atomist.param.SimpleParameterValues
import com.atomist.rug.DefaultRugPipeline
import com.atomist.rug.InterpreterRugPipeline.DefaultRugArchive
import com.atomist.source.{EmptyArtifactSource, SimpleFileBasedArtifactSource, StringFileArtifact}
import org.scalatest.{FlatSpec, Matchers}

class YmlViewTest extends FlatSpec with Matchers {

  import com.atomist.rug.TestUtils._

  it should "update package name with native Rug function" in {
    // val edited = updateWith(prog)
  }

  // Return new content
  private def updateWith(prog: String, yml: String): String = {
    val filename = "thing.yml"
    val as = new SimpleFileBasedArtifactSource("name",
      Seq(
        StringFileArtifact(filename, yml)
      )
    )
    val newName = "Foo"
    val pas = new SimpleFileBasedArtifactSource(DefaultRugArchive, StringFileArtifact(new DefaultRugPipeline().defaultFilenameFor(prog), prog))

    val r = doModification(pas, as, EmptyArtifactSource(""), SimpleParameterValues( Map(
      "new_name" -> newName
    )))

    val f = r.findFile(filename).get
    f.content.contains(s"$newName") should be(true)
    f.content
  }
}
