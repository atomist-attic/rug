package com.atomist.rug.kind.java.spring

import com.atomist.param.SimpleParameterValues
import com.atomist.parse.java.spring.ApplicationYamlAssertions
import com.atomist.project.edit.SuccessfulModification
import com.atomist.rug.kind.java.ExtractApplicationProperties
import com.atomist.rug.kind.java.support.JavaAssertions
import com.atomist.source.{ArtifactSource, ArtifactSourceUtils, EmptyArtifactSource, StringFileArtifact}
import com.atomist.tree.content.project.Configuration
import com.typesafe.scalalogging.LazyLogging
import org.scalatest.{FlatSpec, Matchers}

class ApplicationPropertiesToApplicationYamlEditorTest extends FlatSpec with Matchers with LazyLogging {

  val eap = new ExtractApplicationProperties(JavaAssertions.ApplicationPropertiesFilePath)

  val SpringDocsSampleFile = StringFileArtifact(JavaAssertions.ApplicationPropertiesFilePath,
    """
      |spring.application.name=cruncher
      |spring.datasource.driverClassName=com.mysql.jdbc.Driver
      |spring.datasource.url=jdbc:mysql://localhost/test
      |server.port=9000
    """.stripMargin
  )

  val SpringDocsOutputYamlFile = StringFileArtifact(ApplicationYamlAssertions.ApplicationYamlFilePath,
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

  "ApplicationPropertiesToApplicationYamlEditor" should "not be applicable to empty ArtifactSource" in {
    assert(ApplicationPropertiesToApplicationYamlEditor.applicability(new EmptyArtifactSource("")).canApply === false)
  }

  it should "transform Spring docs sample" in testAgainst(SpringDocsSampleArtifactSource)

  it should "delete application.properties" in {
    val result = testAgainst(SpringDocsSampleArtifactSource)
    result.findFile(JavaAssertions.ApplicationPropertiesFilePath) should not be defined
  }

  it should "construct an application.yml" in {
    val result = testAgainst(SpringDocsSampleArtifactSource)
    result.findFile(ApplicationYamlAssertions.ApplicationYamlFilePath) should be(defined)
  }

  it should "construct an application.yaml with the correct contents" in {
    val result = testAgainst(SpringDocsSampleArtifactSource)
    val aYaml = result.findFile(ApplicationPropertiesToApplicationYamlEditor.ApplicationYamlPath)
    aYaml.get.content shouldBe SpringDocsOutputYamlFile.content
  }

  private  def testAgainst(as: ArtifactSource): ArtifactSource = {
    // Read config first for comparison
    // Wouldn't normally call get without checking, but if it fails the test that's fine
    val config = eap(as.findFile(JavaAssertions.ApplicationPropertiesFilePath).get)

    val mr = ApplicationPropertiesToApplicationYamlEditor.modify(as, SimpleParameterValues.Empty)
    mr match {
      case sma: SuccessfulModification =>
        val aYaml = sma.result.findFile(ApplicationPropertiesToApplicationYamlEditor.ApplicationYamlPath)
        aYaml should be(defined)
        // TODO assertions about ayml
        logger.debug(aYaml.get.content)
        validateYamlRepresentationOfConfiguration(aYaml.get.content, config)
        sma.result
      case whatInGodsHolyNameAreYouBlatheringAbout =>
        logger.debug(ArtifactSourceUtils.prettyListFiles(as))
        fail(s"Unexpected modification result $whatInGodsHolyNameAreYouBlatheringAbout")
    }
  }

  private  def validateYamlRepresentationOfConfiguration(yamlString: String, config: Configuration): Unit = {
    logger.debug(s"Config length = ${config.configurationValues.size}, yaml=[$yamlString]")
    // yamlString should not equal ""
    // compare to expected yaml
  }
}
