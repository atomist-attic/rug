package com.atomist.rug.kind.yaml

import com.atomist.param.SimpleParameterValues
import com.atomist.source.{EmptyArtifactSource, SimpleFileBasedArtifactSource, StringFileArtifact}
import org.scalatest.{FlatSpec, Matchers}

class YamlViewTest extends FlatSpec with Matchers {

  import com.atomist.rug.TestUtils._

  it should "update package name with native Rug function" in {
    // val edited = updateWith(prog)
  }

  // Return new content
//  private def updateWith(prog: String, yaml: String): String = {
//    val filename = "thing.yml"
//    val as = new SimpleFileBasedArtifactSource("name",
//      Seq(
//        StringFileArtifact(filename, yaml)
//      )
//    )
//    val newName = "Foo"
//    val pas = new SimpleFileBasedArtifactSource(DefaultRugArchive, StringFileArtifact(new DefaultRugPipeline().defaultFilenameFor(prog), prog))
//
//    val r = doModification(pas, as, EmptyArtifactSource(""), SimpleParameterValues( Map(
//      "new_name" -> newName
//    )))
//
//    val f = r.findFile(filename).get
//    f.content.contains(s"$newName") should be(true)
//    f.content
//  }
}
