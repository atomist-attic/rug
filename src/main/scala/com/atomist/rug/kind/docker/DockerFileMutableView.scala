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
class DockerFileMutableView(originalBackingObject: FileArtifact, pmv: ProjectMutableView)
  extends LazyFileArtifactBackedMutableView(originalBackingObject, pmv) {

  var _content: Dockerfile = DockerfileParser.parse(originalBackingObject.content)

  def currentContent: String = _content.toString

  override def childNodeNames: Set[String] = Set()

  @ExportFunction(readOnly = true, description = "Return last FROM line in Dockerfile.  It returns the empty string if there is no FROM line.")
  def getFrom: String = _content.getFrom match {
    case Some(s) => s
    case None => ""
  }

  @ExportFunction(readOnly = false, description = "Add or update FROM instruction")
  def addOrUpdateFrom(@ExportFunctionParameterDescription(name = "fromContents",
    description = "The contents of the FROM instruction") fromContents: String) {
    _content.addOrUpdateFrom(fromContents)
  }

  @ExportFunction(readOnly = true, description = "")
  def getExposedPorts: java.util.List[Int] = {
    val exposePorts: Set[Int] = _content.getExposePorts
    /*
    We have to export collections as Java collections, as these get passed into
    nashorn for the typescript stuff, and it doesn't understand scala.
    I'd also rather return a java.util.Set here, but so far our typescript support
    doesn't seem to like that - only java.util.List is supported
     */
    exposePorts.toSeq.asJava
  }

  @ExportFunction(readOnly = false, description = "Add or update EXPOSE instruction")
  def addOrUpdateExpose(@ExportFunctionParameterDescription(name = "exposeContents",
    description = "The contents of the EXPOSE instruction") exposeContents: String) {
    _content.addOrUpdateExpose(exposeContents)
  }

  @ExportFunction(readOnly = false, description = "Add EXPOSE instruction")
  def addExpose(@ExportFunctionParameterDescription(name = "exposeContents",
    description = "The contents of the EXPOSE instruction") exposeContents: String) {
    _content.addExpose(exposeContents)
  }

  @ExportFunction(readOnly = true, description = "Return last MAINTAINER line in Dockerfile.  It returns the empty string if there is no MAINTAINER line.")
  def getMaintainer: String = _content.getMaintainer match {
    case Some(s) => s
    case None => ""
  }

  @ExportFunction(readOnly = false, description = "Add or update MAINTAINER instruction")
  def addOrUpdateMaintainer(@ExportFunctionParameterDescription(name = "maintainerName",
    description = "The name of the MAINTAINER instruction") maintainerName: String,
                            @ExportFunctionParameterDescription(name = "maintainerEmail",
                              description = "The email of the MAINTAINER instruction") maintainerEmail: String) {
    maintainerEmail match {
      case s: String => _content.addMaintainer(s"$maintainerName <$s>")
      case _ => _content.addMaintainer(maintainerName)
    }
  }

  @ExportFunction(readOnly = false, description = "Add MAINTAINER instruction")
  def addMaintainer(@ExportFunctionParameterDescription(name = "maintainerName",
    description = "The name of the MAINTAINER instruction") maintainerName: String,
                    @ExportFunctionParameterDescription(name = "maintainerEmail",
                      description = "The email of the MAINTAINER instruction") maintainerEmail: String) {
    maintainerEmail match {
      case s: String => _content.addMaintainer(s"$maintainerName <$s>")
      case _ => _content.addMaintainer(maintainerName)
    }
  }

  @ExportFunction(readOnly = true,
    description = "Return map of labels.  If there are no labels, an empty map is returned.")
  def getLabels: Map[String, String] = _content.getLabels

  @ExportFunction(readOnly = false, description = "Add or update LABEL instruction")
  def addOrUpdateLabel(@ExportFunctionParameterDescription(name = "labelContents",
    description = "The contents of the LABEL instruction") labelContents: String) {
    _content.addOrUpdateLabel(labelContents)
  }

  @ExportFunction(readOnly = false, description = "Add LABEL instruction")
  def addLabel(@ExportFunctionParameterDescription(name = "labelContents",
    description = "The contents of the LABEL instruction") labelContents: String) {
    _content.addLabel(labelContents)
  }

  @ExportFunction(readOnly = true,
    description = "Return sequence of RUN instructions as strings. If the RUN instruction is an array, a string representation of the array is included. If there are no RUN instructions, an empty sequence is returned.")
  def getRuns: Seq[String] = Dockerfile.seqStringOrArrayToString(_content.getRuns)

  @ExportFunction(readOnly = false, description = "Add RUN instruction")
  def addRun(@ExportFunctionParameterDescription(name = "runContents",
    description = "The contents of the RUN instruction") runContents: String) {
    _content.addRun(runContents)
  }

  @ExportFunction(readOnly = true,
    description = "Return sequence of COPY instructions as a sequence of strings. If there are no COPY instructions, an empty sequence is returned.")
  def getCopies: Seq[Seq[String]] = _content.getCopies

  @ExportFunction(readOnly = false, description = "Add COPY instruction")
  def addCopy(@ExportFunctionParameterDescription(name = "copyContents",
    description = "The contents of the COPY instruction") copyContents: String) {
    _content.addCopy(copyContents)
  }

  @ExportFunction(readOnly = true,
    description = "Return sequence of ADD instructions as a sequence of strings. If there are no ADD instructions, an empty sequence is returned.")
  def getAdds: Seq[Seq[String]] = _content.getAdds

  @ExportFunction(readOnly = false, description = "Add ADD instruction")
  def addAdd(@ExportFunctionParameterDescription(name = "addContents",
    description = "The contents of the ADD instruction") addContents: String) {
    _content.addAdd(addContents)
  }

  @ExportFunction(readOnly = true,
    description = "Return map of environment variables.  If there are no environment variables, an empty map is returned.")
  def getEnvs: Map[String, String] = _content.getEnvs

  @ExportFunction(readOnly = false, description = "Add ENV instruction")
  def addEnv(@ExportFunctionParameterDescription(name = "envContents",
    description = "The contents of the Env instruction") envContents: String) {
    _content.addEnv(envContents)
  }

  @ExportFunction(readOnly = true,
    description = "Return sequence of VOLUME instructions as a sequence of strings. If there are no VOLUME instructions, an empty sequence is returned.")
  def getVolumes: Seq[Seq[String]] = _content.getVolumes

  @ExportFunction(readOnly = false, description = "Add VOLUME instruction")
  def addVolume(@ExportFunctionParameterDescription(name = "volumeContents",
    description = "The contents of the VOLUME instruction") volumeContents: String) {
    _content.addVolume(volumeContents)
  }

  @ExportFunction(readOnly = true,
    description = "Return last WORKDIR instruction in Dockerfile.  It returns the empty string if there is no WORKDIR line.")
  def getWorkdir: String = _content.getWorkdir match {
    case Some(s) => s
    case None => ""
  }

  @ExportFunction(readOnly = false, description = "Add or update WORKDIR instruction")
  def addOrUpdateWorkdir(@ExportFunctionParameterDescription(name = "workdirContents",
    description = "The contents of the WORKDIR instruction") workdirContents: String) {
    _content.addOrUpdateWorkdir(workdirContents)
  }

  @ExportFunction(readOnly = true,
    description = "Return last ENTRYPOINT instruction as a string. If the ENTRYPOINT instruction is an array, a string representation of the Dockerfile array is returned. If there are no ENTRYPOINT instructions, an empty string is returned.")
  def getEntryPoint: String = Dockerfile.stringOrArrayToString(_content.getEntryPoint)

  @ExportFunction(readOnly = false, description = "Add or update ENTRYPOINT instruction")
  def addOrUpdateEntryPoint(@ExportFunctionParameterDescription(name = "entrypointContent",
    description = "The contents of the ENTRYPOINT instruction") entrypointContent: String) {
    _content.addOrUpdateEntryPoint(entrypointContent)
  }

  @ExportFunction(readOnly = true,
    description = "Return last CMD instruction as a string. If the CMD instruction is an array, a string representation of the Dockerfile array is returned. If there are no CMD instructions, an empty string is returned.")
  def getCmd: String = Dockerfile.stringOrArrayToString(_content.getCmd)

  @ExportFunction(readOnly = false, description = "Add or update CMD instruction")
  def addOrUpdateCmd(@ExportFunctionParameterDescription(name = "cmdContents",
    description = "The contents of the CMD instruction") cmdContents: String) {
    _content.addOrUpdateCmd(cmdContents)
  }

  @ExportFunction(readOnly = true,
    description = "Return last HEALTHCHECK instruction in Dockerfile.  It returns the empty string if there is no HEALTCHCHECK line.")
  def getHealthcheck: String = _content.getHealthCheck match {
    case Some(s) => s
    case None => ""
  }

  @ExportFunction(readOnly = false, description = "Add or update HEALTHCHECK instruction")
  def addOrUpdateHealthcheck(@ExportFunctionParameterDescription(name = "healthcheckContent",
    description = "The contents of the HEALTHCHECK instruction") healthcheckContent: String) {
    _content.addOrUpdateHealthcheck(healthcheckContent)
  }
}
