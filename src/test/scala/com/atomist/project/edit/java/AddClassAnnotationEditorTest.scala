package com.atomist.project.edit.java

import com.atomist.param.SimpleParameterValues
import com.atomist.parse.java.ParsingTargets
import com.atomist.project.edit.{FailedModificationAttempt, SuccessfulModification}
import com.atomist.rug.kind.java.AddClassAnnotationEditor
import org.scalatest.{FlatSpec, Matchers}

class AddClassAnnotationEditorTest extends FlatSpec with Matchers {

  // Adds a Mysterious annotation to any class
  val addFoobarAnnotationEditor = new AddClassAnnotationEditor(
    coit => true,
    annotationPackageName = Some("com.megacorp"),
    annotationName = "Mysterious",
    javaSourcePath = ""
  )

  val args = SimpleParameterValues.Empty

  it should "apply annotations where needed" in {
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

  it should "recognize that annotation is applied using FQN without import" is pending
}
