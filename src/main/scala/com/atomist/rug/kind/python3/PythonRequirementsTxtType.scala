package com.atomist.rug.kind.python3

import com.atomist.project.ProjectOperationArguments
import com.atomist.rug.RugRuntimeException
import com.atomist.rug.kind.core.{LazyFileArtifactBackedMutableView, ProjectMutableView}
import com.atomist.rug.kind.dynamic.MutableContainerMutableView
import com.atomist.rug.kind.python3.PythonFileType._
import com.atomist.rug.parser.Selected
import com.atomist.rug.runtime.rugdsl.{DefaultEvaluator, Evaluator}
import com.atomist.rug.spi._
import com.atomist.source.{ArtifactSource, FileArtifact}

class RequirementsType(
                        evaluator: Evaluator
                      )
  extends Type(evaluator)
    with ReflectivelyTypedType {

  def this() = this(DefaultEvaluator)

  override def description = "Python requirements file"

  override def viewManifest: Manifest[MutableContainerMutableView] = manifest[MutableContainerMutableView]

  override protected def findAllIn(rugAs: ArtifactSource,
                                   selected: Selected,
                                   context: MutableView[_],
                                   poa: ProjectOperationArguments,
                                   identifierMap: Map[String, Object]): Option[Seq[MutableView[_]]] = {
    context match {
      case pmv: ProjectMutableView =>
        Some(pmv.currentBackingObject
          .files
          // Is this required?
          .filter(f => f.name.endsWith(".txt"))
          .map(f => toView(f, pmv))
        )
      case _ => None
    }
  }

  protected def toView(f: FileArtifact, pmv: ProjectMutableView): MutableView[_] = {
    new RequirementsTxtMutableView(f, pmv)
  }
}

/**
  * Type for Python requirements.txt
  *
  * @param evaluator used to evaluate expressions
  */
class PythonRequirementsTxtType(
                           evaluator: Evaluator
                         )
  extends RequirementsType(evaluator) {

  def this() = this(DefaultEvaluator)

  override def description = "Python requirements text file"

  override protected def findAllIn(rugAs: ArtifactSource, selected: Selected, context: MutableView[_],
                                   poa: ProjectOperationArguments,
                                   identifierMap: Map[String, Object]): Option[Seq[MutableView[_]]] = {
    context match {
      case pmv: ProjectMutableView =>
        Some(pmv.currentBackingObject
          .findFile(RequirementsTextPath)
          .toSeq
          .map(f => toView(f, pmv))
        )
      case _ => None
    }
  }
}

class RequirementsTxtMutableView(
                                  originalBackingObject: FileArtifact,
                                  parent: ProjectMutableView)
  extends LazyFileArtifactBackedMutableView(originalBackingObject, parent) {

  lazy val originalParsed = RequirementsTxtParser.parseFile(originalBackingObject.content)

  private var currentParsed = originalParsed

  override def dirty = true

  override protected def currentContent: String = currentParsed.value

  override val childNodeNames: Set[String] = Set(RequirementAlias)

  override def childrenNamed(fieldName: String): Seq[MutableView[_]] = fieldName match {
    case RequirementAlias =>
      val reqs = currentParsed.requirements
      reqs.map(r => new RequirementMutableView(r, this))
    case _ => throw new RugRuntimeException(null, s"No child with name '$fieldName' in ${getClass.getSimpleName}")
  }

  //  @ExportFunction(readOnly = false, description = "Append content")
  //  def append(newContent: String): Unit = {
  //    val appended = currentContent + "\n" + newContent
  //    currentParsed = pythonParser.parse(appended)
  //  }

}

class RequirementMutableView(requirement: Requirement, parent: RequirementsTxtMutableView)
  extends ViewSupport[Requirement](requirement, parent) {

  override def childNodeNames: Set[String] = Set()

  override def childrenNamed(fieldName: String): Seq[MutableView[_]] = Nil

  override def childNodeTypes: Set[String] = Set()

  override def nodeName: String = RequirementAlias

  @ExportFunction(readOnly = false, description = "Set version")
  def setVersion(
                  @ExportFunctionParameterDescription(name = "newVersion",
                    description = "New version to set")
                  newVersion: String): Unit = {
    requirement.update(newVersion)
  }

}
