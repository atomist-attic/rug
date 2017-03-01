package com.atomist.tree.content.text.microgrammar

import com.atomist.param.SimpleParameterValues
import com.atomist.rug.TestUtils
import com.atomist.source.{SimpleFileBasedArtifactSource, StringFileArtifact}
import com.typesafe.scalalogging.LazyLogging
import org.scalatest.{FlatSpec, Matchers}

class SecretsTest extends FlatSpec with Matchers with LazyLogging {

  val inYaml =
    """
      |artifactory:
      |  artifactory_base: ${secret.templates.uri:https://sforzando.artifactoryonline.com/sforzando}
      |  public_templates: ${secret.templates.public:public-templates-dev}
      |  private_templates: ${secret.templates.private:private-templates-dev}
    """.stripMargin

  val inJava =
    """
      |public class Foo {
      |
      |@Value("${secret.templates.bar}")
      |private Bar bar;
      |
      |private Ignore me;
      |
      |@Value("${secret.templates.thing}")
      |public setThing(Thing t) {
      |
      |}
      |
      |}
    """.stripMargin

  it should "regexp" in {
    val r = """\$\{secret\.([^\}]+)\}"""
    val m = r.r.findAllMatchIn(inYaml)
    for (x <- m) {
      logger.debug(x.group(1))
    }
  }

  it should "review secrets" in {
    val target = new SimpleFileBasedArtifactSource("",
      StringFileArtifact("application.yml", inYaml))
    val ed = TestUtils.reviewerInSideFile(this, "FindSecrets.ts")
    val rr = ed.review(target, SimpleParameterValues.Empty)
    assert(rr.comments.size === 3)
  }
}
