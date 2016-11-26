package com.atomist.rug.kind.service

import com.atomist.project.{SimpleProjectOperationArguments, ProjectOperationArguments}
import com.atomist.project.archive.{AtomistConfig, DefaultAtomistConfig}
import com.atomist.project.edit.{NoModificationNeeded, SuccessfulModification, ProjectEditor}
import com.atomist.rug.RugRuntimeException
import com.atomist.rug.kind.core.ProjectMutableView
import com.atomist.rug.parser.{RunOtherOperation, Selected}
import com.atomist.rug.runtime.{DefaultEvaluator, Evaluator}
import com.atomist.rug.spi._
import com.atomist.source.ArtifactSource

class ServicesType(
                    evaluator: Evaluator
                  )
  extends Type(evaluator)
    with ReflectivelyTypedType {

  def this() = this(DefaultEvaluator)

  override def name = "services"

  override def description =
    """
      |Type for services. Used in executors.
    """.stripMargin

  override def viewManifest: Manifest[ServicesMutableView] = manifest[ServicesMutableView]

  override protected def findAllIn(rugAs: ArtifactSource, selected: Selected,
                                   context: MutableView[_], poa: ProjectOperationArguments,
                                   identifierMap: Map[String, Object]): Option[Seq[MutableView[_]]] = {
    context match {
      case s: ServicesMutableView => Some(s.children("service"))
    }
  }
}

class ServiceMutableView(override val parent: ServicesMutableView,
                         rugAs: ArtifactSource,
                         val service: Service,
                         atomistConfig: AtomistConfig = DefaultAtomistConfig)
  extends ProjectMutableView(rugAs, service.project, atomistConfig) {

  @ExportFunction(readOnly = true, description = "Raise issue in this service's issue tracker")
  def raiseIssue(name: String): Unit = {
    service.issueRouter.raiseIssue(service, Issue(name))
  }

  @ExportFunction(readOnly = true, description = "Send a message to the service channel")
  def messageChannel(msg: String): Unit = {
    service.userMessageRouter.messageServiceChannel(service, s"$name: $msg")
  }

  // TODO this needs to be fixed
//  @ExportFunction(readOnly = true, description = "Send a message to the current user")
//  def messageUser(msg: String): Unit = {
//    service.userMessageRouter.messageUser(service, "xxxx", s"$name: $msg")
//  }

  def updateTo(newBackingObject: ArtifactSource, roo: RunOtherOperation): Unit = {
    super.updateTo(newBackingObject)
    service.update(newBackingObject, roo.name)
  }

  override def updateTo(newBackingObject: ArtifactSource): Unit = {
    super.updateTo(newBackingObject)
    service.update(newBackingObject, "Executor")
  }

  /**
    * For use by scripts. Edit the project with the given
    * map of string arguments.
    *
    * @param editorName name of editor to use
    * @param params     parameters to pass to the editor
    * @return
    */
  override def editWith(editorName: String,
               params: Map[String, Object]): Unit =
    super.editWith(editorName, params, parent.serviceSource.projectOperations)

  // TODO parameter handling
  @ExportFunction(readOnly = false, description = "Edit project with the named editor")
  def editUsing(editorName: String): Unit = {
    editWith(editorName, Map[String, String]())
  }
}
