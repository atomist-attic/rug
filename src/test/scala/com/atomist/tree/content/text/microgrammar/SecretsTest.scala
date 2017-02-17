package com.atomist.tree.content.text.microgrammar

import com.atomist.param.SimpleParameterValues
import com.atomist.project.review.ProjectReviewer
import com.atomist.rug.DefaultRugPipeline
import com.atomist.rug.InterpreterRugPipeline.DefaultRugArchive
import com.atomist.source.{SimpleFileBasedArtifactSource, StringFileArtifact}
import com.typesafe.scalalogging.LazyLogging
import org.scalatest.{FlatSpec, Matchers}

class SecretsTest extends FlatSpec with Matchers with LazyLogging {

  val inYml =
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
    val m = r.r.findAllMatchIn(inYml)
    for (x <- m) {
      logger.debug(x.group(1))
    }
  }

  it should "review secrets" in {
    val prog =
      """
        |reviewer FindSecrets
        |
        |#let secret = "\$\{secret\.([^\}]+)\}"
        |
        |with File f when { f.name().endsWith('yml') }
        |	do eval {
        |     var secret = /\$\{secret\.([^\}]+)\}/g;
        |     var matches = f.content().match(secret);
        |     for ( i = 0; i < matches.length; i++)
        |       f.majorProblem(matches[i], ic);
        |     return null;
        | }
      """.stripMargin
    val rp = new DefaultRugPipeline
    val as = new SimpleFileBasedArtifactSource(DefaultRugArchive, StringFileArtifact(rp.defaultFilenameFor(prog), prog))
    val ed = rp.create(as,  None).head.asInstanceOf[ProjectReviewer]

    val target = new SimpleFileBasedArtifactSource("",
      StringFileArtifact("application.yml", inYml))
    val rr = ed.review(target, SimpleParameterValues.Empty)
    assert(rr.comments.size === 3)
  }
}
