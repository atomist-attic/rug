package com.atomist.rug

import com.atomist.param.SimpleParameterValues
import com.atomist.parse.java.ParsingTargets
import com.atomist.project.edit.{ProjectEditor, SuccessfulModification}
import com.atomist.rug.InterpreterRugPipeline.DefaultRugArchive
import com.atomist.source.{SimpleFileBasedArtifactSource, StringFileArtifact}
import org.scalatest.{FlatSpec, Matchers}

/**
  * Tests extract from syntax
  */
class PathExtractionTest extends FlatSpec with Matchers {

  it should "extract simple value" in {
    val project = ParsingTargets.NewStartSpringIoProject
    val prog =
      """
         |editor First
         |
         |let f = $(/src//*[@name='application.properties'])
         |
         |with f
         |  do append "foo=bar"
      """.stripMargin
    val rp = new DefaultRugPipeline

    val as = new SimpleFileBasedArtifactSource(DefaultRugArchive, StringFileArtifact(rp.defaultFilenameFor(prog), prog))
    val ed = rp.create(as).head

    // Check it works OK with these parameters
    ed.asInstanceOf[ProjectEditor].modify(project, SimpleParameterValues.Empty) match {
      case sm: SuccessfulModification =>
        val f = sm.result.findFile("src/main/resources/application.properties").get
        assert(f.content === "foo=bar")
      case _ => ???
    }
  }

  it should "follow path to node and change type" in {
    val project = ParsingTargets.NewStartSpringIoProject
    val prog =
      """
         |editor First
         |
         |let m = $(/src/main/java/com/example/JavaType()[@name='DemoApplication']/*[@type='method']).name
         |
         |with File when path = "src/main/resources/application.properties"
         |  do append m
      """.stripMargin
    val rp = new DefaultRugPipeline

    val rugAs = new SimpleFileBasedArtifactSource(DefaultRugArchive, StringFileArtifact(rp.defaultFilenameFor(prog), prog))

    val ed = rp.create(rugAs).head

    // Check it works OK with these parameters
    ed.asInstanceOf[ProjectEditor].modify(project, SimpleParameterValues.Empty)
    match {
      case sm: SuccessfulModification =>
        val f = sm.result.findFile("src/main/resources/application.properties").get
        f.content.contains("main") should be (true)
      case _ => ???
    }
  }

  it should "self or descend into type" in pendingUntilFixed {
    val project = ParsingTargets.NewStartSpringIoProject
    val prog =
      """
        |editor First
        |
        |let m = $(/src/main/java//*:java.class[name='DemoApplication']/[type='method']).name
        |
        |with File when path = "src/main/resources/application.properties"
        |  do append m
      """.stripMargin
    val rp = new DefaultRugPipeline

    val rugAs = new SimpleFileBasedArtifactSource("", StringFileArtifact("editor/LineCommenter.rug", prog))

    val ed = rp.create(rugAs).head

    // Check it works OK with these parameters
    ed.asInstanceOf[ProjectEditor].modify(project, SimpleParameterValues.Empty)
    match {
      case sm: SuccessfulModification =>
        val f = sm.result.findFile("src/main/resources/application.properties").get
        f.content.contains("main") should be (true)
      case _ => ???
    }
  }
}
