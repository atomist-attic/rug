package com.atomist.rug.kind.python3

import com.atomist.param.SimpleParameterValues
import com.atomist.project.edit.{ModificationAttempt, NoModificationNeeded, ProjectEditor, SuccessfulModification}
import com.atomist.rug.DefaultRugPipeline
import com.atomist.rug.kind.DefaultTypeRegistry
import com.atomist.source.{ArtifactSource, SimpleFileBasedArtifactSource, StringFileArtifact}
import org.scalatest.{FlatSpec, Matchers}

// TODO This test is commented out as these tests need to be rewritten to be valid
abstract class RequirementsTxtTypeUsageTest extends FlatSpec with Matchers {

  import RequirementsTxtParserTest._

  val Simple: ArtifactSource = SimpleFileBasedArtifactSource(
      StringFileArtifact("requirements.txt", simple1)
    )

  def exec(program: String, as: ArtifactSource, params: Map[String, String] = Map()): ModificationAttempt = {
    val runtime = new DefaultRugPipeline(DefaultTypeRegistry)
    val as = new SimpleFileBasedArtifactSource("", StringFileArtifact("editor/LineCommenter.rug", program))
    val eds = runtime.create(as,None)
    assert(eds.size === 1)
    val pe = eds.head.asInstanceOf[ProjectEditor]
    pe.modify(as, SimpleParameterValues( params))
  }

  def modifyRequirementsTxtAndReparseSuccessfully(program: String, as: ArtifactSource, params: Map[String, String] = Map()): ArtifactSource = {
    exec(program, as, params) match {
      case sm: SuccessfulModification =>
        sm.result.allFiles
          //.filter(_.name.endsWith(PythonExtension))
          .map(py => RequirementsTxtParser.parseFile(py.content))
          .map(tree => tree.childNodes.nonEmpty)
        sm.result
      case _ => ???
    }
  }

  it should "parse requirements.txt and iterate over requirements" in {
    val prog =
      """
        |editor ParseRequirementsTxt
        |
        |with PythonRequirementsTxt
        | with requirement
        |   do setVersion "2.5"
      """.stripMargin
    val r = exec(prog, Simple)
    r match {
      case sm: SuccessfulModification =>
        val f = sm.result.findFile("requirements.txt").get
        f.content.contains("2.5") should be(true)
      case _ => ???
    }
  }

  it should "parse requirements.txt in other .txt file and iterate over requirements" in {
    val withCustomNamedRequirementsFile: ArtifactSource = SimpleFileBasedArtifactSource(
      StringFileArtifact("other.txt", simple1)
    )

    val prog =
      """
        |editor ParseRequirementsTxt
        |
        |with PythonRequirementsTxt when path = "other.txt"
        | with requirement
        |   do setVersion "2.5"
      """.stripMargin
    val r = exec(prog, withCustomNamedRequirementsFile)
    r match {
      case sm: SuccessfulModification =>
        val f = sm.result.findFile("other.txt").get
        f.content.contains("2.5") should be(true)
      case _ => ???
    }
  }

  it should "handle invalid requirements.txt format" in pendingUntilFixed {
    val bad = SimpleFileBasedArtifactSource(
      StringFileArtifact("other.txt", "This is a load of nonsense")
    )

    val prog =
      """
        |editor ParseRequirementsTxt
        |
        |with PythonRequirementsTxt when path = "other.txt"
        | with requirement
        |   do setVersion "2.5"
      """.stripMargin
    val r = exec(prog, bad)
    r match {
      case nm: NoModificationNeeded =>
      
      case _ => ???
    }
  }

}
