package com.atomist.rug.kind.java.spring

import com.atomist.parse.java.spring.ApplicationYmlAssertions
import com.atomist.tree.content.project.Configuration
import com.atomist.project.SimpleProjectOperationArguments
import com.atomist.project.edit.SuccessfulModification
import com.atomist.rug.kind.java.ExtractApplicationProperties
import com.atomist.source.{ArtifactSource, ArtifactSourceUtils, EmptyArtifactSource, StringFileArtifact}
import com.typesafe.scalalogging.LazyLogging
import org.scalatest.{FlatSpec, Matchers}

class ApplicationPropertiesToApplicationYmlEditorTest extends FlatSpec with Matchers with LazyLogging {

  val eap = new ExtractApplicationProperties(ApplicationPropertiesAssertions.ApplicationPropertiesFilePath)

  val SpringDocsSampleFile = StringFileArtifact(ApplicationPropertiesAssertions.ApplicationPropertiesFilePath,
    """
      |spring.application.name=cruncher
      |spring.datasource.driverClassName=com.mysql.jdbc.Driver
      |spring.datasource.url=jdbc:mysql://localhost/test
      |server.port=9000
    """.stripMargin
  )

  val SpringDocsOutputYmlFile = StringFileArtifact(ApplicationYmlAssertions.ApplicationYmlFilePath,
    """spring:
      |  application:
      |    name: cruncher
      |  datasource:
      |    driverClassName: com.mysql.jdbc.Driver
      |    url: jdbc:mysql://localhost/test
      |server:
      |  port: 9000
      |""".stripMargin
  )

  val SpringDocsSampleArtifactSource = EmptyArtifactSource("") + SpringDocsSampleFile

  "ApplicationPropertiesToApplicationYmlEditor" should "not be applicable to empty ArtifactSource" in {
    ApplicationPropertiesToApplicationYmlEditor.applicability(new EmptyArtifactSource("")).canApply should equal(false)
  }

  it should "transform Spring docs sample" in testAgainst(SpringDocsSampleArtifactSource)

  it should "delete application.properties" in {
    val result = testAgainst(SpringDocsSampleArtifactSource)
    result.findFile(ApplicationPropertiesAssertions.ApplicationPropertiesFilePath) should not be defined
  }

  it should "construct an application.yml" in {
    val result = testAgainst(SpringDocsSampleArtifactSource)
    result.findFile(ApplicationYmlAssertions.ApplicationYmlFilePath) should be(defined)
  }

  it should "construct an application.yml with the correct contents" in {
    val result = testAgainst(SpringDocsSampleArtifactSource)
    val ayml = result.findFile(ApplicationPropertiesToApplicationYmlEditor.ApplicationYmlPath)
    ayml.get.content shouldBe SpringDocsOutputYmlFile.content
  }

  private def testAgainst(as: ArtifactSource): ArtifactSource = {
    // Read config first for comparison
    // Wouldn't normally call get without checking, but if it fails the test that's fine
    val config = eap(as.findFile(ApplicationPropertiesAssertions.ApplicationPropertiesFilePath).get)

    val mr = ApplicationPropertiesToApplicationYmlEditor.modify(as, SimpleProjectOperationArguments.Empty)
    mr match {
      case sma: SuccessfulModification =>
        val ayml = sma.result.findFile(ApplicationPropertiesToApplicationYmlEditor.ApplicationYmlPath)
        ayml should be(defined)
        // TODO assertions about ayml
        logger.debug(ayml.get.content)
        validateYmlRepresentationOfConfiguration(ayml.get.content, config)
        sma.result
      case whatInGodsHolyNameAreYouBlatheringAbout =>
        logger.debug(ArtifactSourceUtils.prettyListFiles(as))
        fail(s"Unexpected modification result $whatInGodsHolyNameAreYouBlatheringAbout")
    }
  }

  private def validateYmlRepresentationOfConfiguration(ymlString: String, config: Configuration): Unit = {
    logger.debug(s"Config length = ${config.configurationValues.size}, yml=[$ymlString]")
    // ymlString should not equal ""
    // compare to expected yaml
  }
}
