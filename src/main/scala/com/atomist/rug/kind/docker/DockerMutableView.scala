package com.atomist.rug.kind.docker

import com.atomist.rug.kind.core.{LazyFileArtifactBackedMutableView, ProjectMutableView}
import com.atomist.rug.spi.{ExportFunction, ExportFunctionParameterDescription}
import com.atomist.source.FileArtifact

import scala.collection.JavaConverters._

/**
  * Rug type for a Docker file
  * @param originalBackingObject original FileArtifact
  * @param pmv owning project
  */
class DockerMutableView(originalBackingObject: FileArtifact, pmv: ProjectMutableView)
  extends LazyFileArtifactBackedMutableView(originalBackingObject, pmv) {

  var _content: Dockerfile = DockerfileParser.parse(originalBackingObject.content)

  def currentContent: String = _content.toString

  override def childNodeNames: Set[String] = Set()

  @ExportFunction(readOnly = true, description = "")
  def getExposedPorts: java.util.List[Int] = {
    val exposePorts: Set[Int] = _content.getExposePorts()
    /*
    We have to export collections as Java collections, as these get passed into
    nashorn for the typescript stuff, and it doesn't understand scala.
    I'd also rather return a java.util.Set here, but so far our typescript support
    doesn't seem to like that - only java.util.List is supported
     */
    exposePorts.toSeq.asJava
  }

  @ExportFunction(readOnly = false, description = "Add or update FROM directive")
  def addOrUpdateFrom(@ExportFunctionParameterDescription(name = "fromContents",
    description = "The contents of the FROM directive") fromContents: String) {
    _content.addOrUpdateFrom(fromContents)
  }

  @ExportFunction(readOnly = false, description = "Add or update EXPOSE directive")
  def addOrUpdateExpose(@ExportFunctionParameterDescription(name = "exposeContents",
    description = "The contents of the EXPOSE directive") exposeContents: String) {
    _content.addOrUpdateExpose(exposeContents)
  }

  @ExportFunction(readOnly = false, description = "Add EXPOSE directive")
  def addExpose(@ExportFunctionParameterDescription(name = "exposeContents",
    description = "The contents of the EXPOSE directive") exposeContents: String) {
    _content.addExpose(exposeContents)
  }

  @ExportFunction(readOnly = false, description = "Add or update MAINTAINER directive")
  def addOrUpdateMaintainer(@ExportFunctionParameterDescription(name = "maintainerName",
    description = "The name of the MAINTAINER directive") maintainerName: String,
                            @ExportFunctionParameterDescription(name = "maintainerEmail",
                              description = "The email of the MAINTAINER directive") maintainerEmail: String) {
    maintainerEmail match {
      case s: String => _content.addMaintainer(s"$maintainerName <$s>")
      case _ => _content.addMaintainer(maintainerName)
    }
  }

  @ExportFunction(readOnly = false, description = "Add MAINTAINER directive")
  def addMaintainer(@ExportFunctionParameterDescription(name = "maintainerName",
    description = "The name of the MAINTAINER directive") maintainerName: String,
                    @ExportFunctionParameterDescription(name = "maintainerEmail",
                      description = "The email of the MAINTAINER directive") maintainerEmail: String) {
    maintainerEmail match {
      case s: String => _content.addMaintainer(s"$maintainerName <$s>")
      case _ => _content.addMaintainer(maintainerName)
    }
  }

  @ExportFunction(readOnly = false, description = "Add or update LABEL directive")
  def addOrUpdateLabel(@ExportFunctionParameterDescription(name = "labelContents",
    description = "The contents of the LABEL directive") labelContents: String) {
    _content.addOrUpdateLabel(labelContents)
  }

  @ExportFunction(readOnly = false, description = "Add LABEL directive")
  def addLabel(@ExportFunctionParameterDescription(name = "labelContents",
    description = "The contents of the LABEL directive") labelContents: String) {
    _content.addLabel(labelContents)
  }

  @ExportFunction(readOnly = false, description = "Add RUN directive")
  def addRun(@ExportFunctionParameterDescription(name = "runContents",
    description = "The contents of the RUN directive") runContents: String) {
    _content.addRun(runContents)
  }

  @ExportFunction(readOnly = false, description = "Add COPY directive")
  def addCopy(@ExportFunctionParameterDescription(name = "copyContents",
    description = "The contents of the COPY directive") copyContents: String) {
    _content.addCopy(copyContents)
  }

  @ExportFunction(readOnly = false, description = "Add ADD directive")
  def addAdd(@ExportFunctionParameterDescription(name = "addContents",
    description = "The contents of the ADD directive") addContents: String) {
    _content.addAdd(addContents)
  }

  @ExportFunction(readOnly = false, description = "Add Env directive")
  def addEnv(@ExportFunctionParameterDescription(name = "envContents",
    description = "The contents of the Env directive") envContents: String) {
    _content.addEnv(envContents)
  }

  @ExportFunction(readOnly = false, description = "Add VOLUME directive")
  def addVolume(@ExportFunctionParameterDescription(name = "volumeContents",
    description = "The contents of the VOLUME directive") volumeContents: String) {
    _content.addVolume(volumeContents)
  }

  @ExportFunction(readOnly = false, description = "Add or update WORKDIR directive")
  def addOrUpdateWorkdir(@ExportFunctionParameterDescription(name = "workdirContents",
    description = "The contents of the WORKDIR directive") workdirContents: String) {
    _content.addOrUpdateWorkdir(workdirContents)
  }

  @ExportFunction(readOnly = false, description = "Add or update ENTRYPOINT directive")
  def addOrUpdateEntryPoint(@ExportFunctionParameterDescription(name = "entrypointContent",
    description = "The contents of the ENTRYPOINT directive") entrypointContent: String) {
    _content.addOrUpdateEntryPoint(entrypointContent)
  }

  @ExportFunction(readOnly = false, description = "Add or update CMD directive")
  def addOrUpdateCmd(@ExportFunctionParameterDescription(name = "cmdContents",
    description = "The contents of the CMD directive") cmdContents: String) {
    _content.addOrUpdateCmd(cmdContents)
  }

  @ExportFunction(readOnly = false, description = "Add or update HEALTHCHECK directive")
  def addOrUpdateHealthcheck(@ExportFunctionParameterDescription(name = "healthcheckContent",
    description = "The contents of the HEALTHCHECK directive") healthcheckContent: String) {
    _content.addOrUpdateHealthcheck(healthcheckContent)
  }
}
