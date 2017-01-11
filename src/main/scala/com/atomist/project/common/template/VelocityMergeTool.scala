package com.atomist.project.common.template

import java.io.StringWriter
import java.nio.charset.Charset

import com.atomist.source.{ArtifactSource, FileArtifact, StringFileArtifact}
import com.typesafe.scalalogging.LazyLogging
import org.apache.velocity.VelocityContext
import org.apache.velocity.app.VelocityEngine

object VelocityMergeTool {

  val TemplateSuffix = ".vm"

  val InPlaceTemplateSuffix = s"_$TemplateSuffix"

  implicit def toVelocityContext(context: MergeContext): VelocityContext = {
    val vContext = new VelocityContext()
    context.map.map {
      case (k, v) => vContext.put(k, v)
    }
    vContext
  }

  implicit def toVelocityContext(m: Map[String, Any]): VelocityContext =
    toVelocityContext(MergeContext(m))
}

class VelocityMergeTool(templateContent: ArtifactSource)
  extends MergeTool
    with LazyLogging {

  // For implicit conversion to Velocity context
  import VelocityMergeTool._

  val ve = new VelocityEngine
  ve.setProperty("resource.loader", "custom")
  ve.setProperty("custom.resource.loader.instance", new ArtifactSourceBackedVelocityResourceLoader(templateContent))
  ve.init()

  override def isTemplate(path: String): Boolean = path.endsWith(TemplateSuffix)

  override def mergeToFile(context: MergeContext, templatePath: String): FileArtifact =
    templateContent.findFile(templatePath) match {
      case None =>
        throw new IllegalArgumentException(s"Template '$templatePath' not found")
      case Some(template) =>
        val templateOutput = new StringWriter
        logger.debug(s"Using template : [$templatePath]")
        if (ve.mergeTemplate(templatePath, Charset.defaultCharset().name(), context, templateOutput)) {
          val filePath = mergeString(context, toInPlaceFilePath(templatePath)).replace(":", "/")
          StringFileArtifact(filePath, templateOutput.toString).withMode(template.mode)
        }
        else throw new InvalidTemplateException(s"failed to merge velocity template: $template")
    }

  override def mergeString(context: MergeContext, templateString: String): String = {
    val templateOutput = new StringWriter
    logger.debug(s"Using String : [$templateString]")
    val logTag = templateString
    ve.evaluate(context, templateOutput, logTag, templateString)
    templateOutput.toString
  }

  private def toInPlaceFilePath(path: String): String =
    path.dropRight(InPlaceTemplateSuffix.length)
}
