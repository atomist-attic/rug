package com.atomist.rug.kind.java.spring

import _root_.java.util

import com.atomist.param.{Parameter, ParameterValues, Tag}
import com.atomist.project.edit._
import com.atomist.rug.kind.java.ExtractApplicationProperties
import com.atomist.rug.kind.java.support.JavaAssertions
import com.atomist.source.{ArtifactSource, FileArtifact, StringFileArtifact}
import com.atomist.tree.content.project.{ConfigValue, Configuration}
import com.atomist.util.yaml.{MapToYamlStringSerializer, PropertiesToMapStructureParser}
import com.typesafe.scalalogging.LazyLogging

/**
  * Convert application.properties to application.yaml
  */
object ApplicationPropertiesToApplicationYamlEditor extends ProjectEditor with LazyLogging {

  import JavaAssertions.ApplicationPropertiesFilePath

  val ApplicationYamlPath = "src/main/resources/application.yml"

  private val configExtractor = new ExtractApplicationProperties(source = ApplicationPropertiesFilePath)

  override def modify(as: ArtifactSource, pmi: ParameterValues): ModificationAttempt = {
    as.findFile(ApplicationPropertiesFilePath).map(f => {
      val config = configExtractor(f)
      val applicationYaml: FileArtifact = StringFileArtifact(ApplicationYamlPath, toYamlString(config))
      val result = as + applicationYaml - ApplicationPropertiesFilePath
      SuccessfulModification(result)
    }).getOrElse(FailedModificationAttempt(s"Did not find application.properties file at $ApplicationPropertiesFilePath in ${as.id}"))
  }

  override def applicability(as: ArtifactSource): Applicability =
    Applicability(JavaAssertions.isSpring(as) && JavaAssertions.hasApplicationProperties(as), "Checked Spring and application.properties")

  override def description: String = "Atomist Core Editor: Convert application.properties to application.yml (application.properties->application.yml)"

  override def name: String = "ApplicationProperties2Yaml"

  override def tags: Seq[Tag] = Seq(
    Tag("spring", "Spring Framework"), Tag("spring-boot", "Spring Boot")
  )

  override def parameters: Seq[Parameter] = Seq()

  def toYamlString(cvs: Configuration): String = {
    logger.debug(s"Parsing configuration $cvs to YAML")

    val yamlMap = new util.HashMap[String, Object]()

    cvs.configurationValues foreach ((configurationValue: ConfigValue) => {
      PropertiesToMapStructureParser.populateYamlForPeriodScopedProperty(configurationValue.name, configurationValue.value, yamlMap)
    })

    MapToYamlStringSerializer.toYamlString(yamlMap)
  }
}
