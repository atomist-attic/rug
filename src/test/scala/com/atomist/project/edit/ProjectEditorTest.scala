package com.atomist.project.edit

import com.atomist.param._
import com.atomist.parse.java.ParsingTargets
import com.atomist.project.common.{IllformedParametersException, MissingParametersException}
import com.atomist.rug.kind.java.AddClassAnnotationEditor
import com.atomist.rug.kind.java.spring.SpringTypeSelectors
import com.atomist.source.{ArtifactSource, EmptyArtifactSource, SimpleFileBasedArtifactSource, StringFileArtifact}
import org.scalatest.{FlatSpec, Matchers}

/**
  * Basic tests for Project Editor semantics
  */
class ProjectEditorTest extends FlatSpec with Matchers {

  // Adds a Mysterious annotation to any class
  val addFoobarAnnotationEditor = new AddClassAnnotationEditor(
    coit => true,
    annotationPackageName = Some("com.megacorp"),
    annotationName = "Mysterious",
    javaSourcePath = ""
  )

  val args = SimpleParameterValues.Empty

  val ed: ProjectEditor = new ProjectEditorSupport {

    override def parameters = Seq(Parameter("class", ParameterValidationPatterns.JavaClass))

    override protected  def modifyInternal(as: ArtifactSource, pmi: ParameterValues): ModificationAttempt = {
      SuccessfulModification(as)
    }

    override def applicability(as: ArtifactSource): Applicability = Applicability.OK

    override def description: String = ""

    override def name: String = ""

    override def tags: Seq[Tag] = Nil
  }

  it should "require required parameters" in {
    an[MissingParametersException] should be thrownBy ed.modify(new EmptyArtifactSource(""), SimpleParameterValues.Empty)
  }

  it should "require valid parameters" in {
    an[IllformedParametersException] should be thrownBy ed.modify(new EmptyArtifactSource(""), new SimpleParameterValues(List(SimpleParameterValue("class", "3xxx")
      )))
  }

  it should "apply edit if needed" in {
    val as = ParsingTargets.SpringIoGuidesRestServiceSource
    assert(addFoobarAnnotationEditor.applicability(as).canApply === true)
    addFoobarAnnotationEditor.modify(as, args) match {
      case sma: SuccessfulModification =>
        val javaFiles = sma.result.files.filter(_.name.endsWith(".java"))
        assert(javaFiles.size === 2)
        javaFiles.foreach(f => {
          f.content contains "@Mysterious" should be(true)
        })
      case f: FailedModificationAttempt => fail
      case _ => fail
    }
  }

  it should "recognize edit unnecessary because change already made is valid" in
    testUnnecessaryEdit(addFoobarAnnotationEditor)

  it should "recognize edit unnecessary because change already made is valid even in failfast mode" in
    testUnnecessaryEdit(new AddClassAnnotationEditor(
      coit => true,
      annotationPackageName = Some("com.megacorp"),
      annotationName = "Mysterious",
      javaSourcePath = ""
    ) {
      override def failOnNoModification: Boolean = true
    })

  private  def testUnnecessaryEdit(ed: ProjectEditor): Unit = {
    val as = new SimpleFileBasedArtifactSource("", Seq(
      StringFileArtifact("SomeClass.java", "import com.megacorp.Mysterious; @Mysterious public class SomeClass {}"),
      StringFileArtifact("SomeInterface.java", "import com.megacorp.Mysterious;  @Mysterious public interface SomeInterface {}")
    ))
    assert(addFoobarAnnotationEditor.applicability(as).canApply === true)
    addFoobarAnnotationEditor.modify(as, args) match {
      case n: NoModificationNeeded =>
      
      case _ => fail
    }
  }

  it should "default to accept impossible edit" in {
    val as = new SimpleFileBasedArtifactSource("", Seq(
      StringFileArtifact("SomeClass.java", "import com.megacorp.Mysterious; @Mysterious public class SomeClass {}"),
      StringFileArtifact("SomeInterface.java", "import com.megacorp.Mysterious;  @Mysterious public interface SomeInterface {}")
    ))
    assert(addFoobarAnnotationEditor.applicability(as).canApply === true)
    addFoobarAnnotationEditor.modify(as, args) match {
      case n: NoModificationNeeded =>
      
      case wtf => fail(s"$wtf not expected")
    }
  }

  it should "be configurable to reject impossible edit" in {
    val neverMatchEditor = new AddClassAnnotationEditor(
      SpringTypeSelectors.FeignProxyClassSelector,
      annotationPackageName = Some("com.megacorp"),
      annotationName = "Mysterious",
      javaSourcePath = ""
    ) {
      override def failOnNoModification: Boolean = true
    }
    val as = new SimpleFileBasedArtifactSource("", Seq(
      StringFileArtifact("SomeClass.java", "import com.megacorp.Mysterious; @Mysterious public class SomeClass {}"),
      StringFileArtifact("SomeInterface.java", "import com.megacorp.Mysterious;  @Mysterious public interface SomeInterface {}")
    ))
    assert(neverMatchEditor.applicability(as).canApply === true)
    neverMatchEditor.modify(as, args) match {
      case n: FailedModificationAttempt =>
      
      case wtf => fail(s"$wtf not expected")
    }
  }
}
