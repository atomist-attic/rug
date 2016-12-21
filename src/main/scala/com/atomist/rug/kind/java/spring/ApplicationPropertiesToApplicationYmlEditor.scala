package com.atomist.rug.kind.java.spring

import _root_.java.util

import com.atomist.param.{Parameter, Tag}
import com.atomist.project.ProjectOperationArguments
import com.atomist.project.edit._
import com.atomist.rug.kind.java.ExtractApplicationProperties
import com.atomist.rug.kind.java.support.JavaAssertions
import com.atomist.source.{ArtifactSource, FileArtifact, StringFileArtifact}
import com.atomist.tree.content.project.{ConfigValue, Configuration}
import com.atomist.util.yml.{MapToYamlStringSerializer, PropertiesToMapStructureParser}
import com.typesafe.scalalogging.LazyLogging

/**
  * Convert application.properties to application.yml
  */
object ApplicationPropertiesToApplicationYmlEditor extends ProjectEditor with LazyLogging {

  import ApplicationPropertiesAssertions.ApplicationPropertiesFilePath

  val ApplicationYmlPath = "src/main/resources/application.yml"

  private val configExtractor = new ExtractApplicationProperties(source = ApplicationPropertiesFilePath)

  override val impacts: Set[Impact] = Set(ConfigImpact)

  override def modify(as: ArtifactSource, pmi: ProjectOperationArguments): ModificationAttempt = {
    as.findFile(ApplicationPropertiesFilePath).map(f => {
      val config = configExtractor(f)
      val applicationYml: FileArtifact = StringFileArtifact(ApplicationYmlPath, toYmlString(config))
      val result = as + applicationYml - ApplicationPropertiesFilePath
      SuccessfulModification(result, impacts, name)
    }).getOrElse(FailedModificationAttempt(s"Did not find application.properties file at $ApplicationPropertiesFilePath in ${as.id}"))
  }

  override def applicability(as: ArtifactSource): Applicability =
    Applicability(JavaAssertions.isSpring(as) && ApplicationPropertiesAssertions.hasApplicationProperties(as), "Checked Spring and application.properties")

  override def description: String = "Atomist Core Editor: Convert application.properties to application.yml (application.properties->application.yml)"

  override def name: String = "ApplicationProperties2Yaml"

  override def group: Option[String] = Some("atomist")

  override def tags: Seq[Tag] = Seq(
    Tag("spring", "Spring Framework"), Tag("spring-boot", "Spring Boot")
  )

  override def parameters: Seq[Parameter] = Seq()

  def toYmlString(cvs: Configuration): String = {
    logger.debug(s"Parsing configuration $cvs to YML")

    val yamlMap = new util.HashMap[String, Object]()

    cvs.configurationValues foreach ((configurationValue: ConfigValue) => {
      PropertiesToMapStructureParser.populateYamlForPeriodScopedProperty(configurationValue.name, configurationValue.value, yamlMap)
    })

    MapToYamlStringSerializer.toYamlString(yamlMap)
  }
}
