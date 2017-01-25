package com.atomist.rug.kind.java.spring

import java.util

import com.atomist.param.ParameterValues
import com.atomist.project.edit._
import com.atomist.project.edit.ProjectEditorSupport
import com.atomist.project.edit.ProjectEditorUtils._
import com.atomist.rug.kind.core.ChangeLogEntry
import com.atomist.source.ArtifactSource
import com.atomist.util.yml.{MapToYamlStringSerializer, PropertiesToMapStructureParser}

/**
  * No parameters are required. All key-value pairs will be added to the
  * application YML file.
  */
object ApplicationYmlKeyAddingEditor extends ProjectEditorSupport {

  import ApplicationPropertiesToApplicationYmlEditor.ApplicationYmlPath

  val YamlHeader = "# Created by Atomist\n\n"

  override protected def modifyInternal(as: ArtifactSource, pmi: ParameterValues): ModificationAttempt = {
    val yamlMap = new util.HashMap[String, Object]()
    val yamlFile = newOrExistingFile(as, ApplicationYmlPath, YamlHeader)

    for {
      pv <- pmi.parameterValues
    } {
      PropertiesToMapStructureParser.populateYamlForPeriodScopedProperty(pv.getName, pv.getValue.toString, yamlMap)
    }

    val stringifiedYamlEntries = MapToYamlStringSerializer.toYamlString(yamlMap)

    val result = appendToFile(as, yamlFile, stringifiedYamlEntries)
    SuccessfulModification(
      result,
      Seq(ChangeLogEntry("Added keys to yaml file", result)))
  }

  /**
    * We can apply to anything. If a YML file doesn't exist, we'll create one.
    *
    * @param as The Artifact Source we're working on
    */
  override def applicability(as: ArtifactSource): Applicability = Applicability.OK

  override def description: String = "Atomist Core Editor: Add key to application YML"

  override def name: String = "ApplicationYmlKeyAddingEditor"
}
