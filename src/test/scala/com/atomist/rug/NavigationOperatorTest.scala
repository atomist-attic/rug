package com.atomist.rug

import com.atomist.project.edit._
import com.atomist.rug.kind.DefaultTypeRegistry
import com.atomist.source.{SimpleFileBasedArtifactSource, StringFileArtifact}
import org.scalatest.{FlatSpec, Matchers}

class NavigationOperatorTest extends FlatSpec with Matchers {

  val simpleAs = new SimpleFileBasedArtifactSource("name",
    Seq(
      StringFileArtifact("filename", "some content")
    )
  )

  it should "allow simple navigation against project" in {
    val prog =
      """
        |editor Test
        |
        |with project begin
        |    let x = filename.content
        |end
      """.stripMargin
    an[BadRugException] should be thrownBy {
      create(prog)
    }
  }

  private def create(prog: String, namespace: Option[String] = None, globals: Seq[ProjectEditor] = Nil): Seq[ProjectEditor] = {
    val runtime = new DefaultRugPipeline(DefaultTypeRegistry)
    runtime.createFromString(prog, namespace, globals).asInstanceOf[Seq[ProjectEditor]]
  }
}