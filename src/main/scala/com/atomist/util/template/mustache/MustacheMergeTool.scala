package com.atomist.util.template.mustache

import java.io.StringWriter

import com.atomist.source.{ArtifactSource, FileArtifact, StringFileArtifact}
import com.atomist.util.template.{MergeContext, MergeTool}
import com.github.mustachejava.DefaultMustacheFactory

import scala.collection.JavaConverters._

object MustacheMergeTool {

  val SupportedExtensions = Set(
    ".scaml",
    ".mustache"
  )
}

class MustacheMergeTool(templateContent: ArtifactSource)
  extends MergeTool {

  import MustacheMergeTool._

  private val mf = new DefaultMustacheFactory(new LocalizedMustacheResolver)

  override def isTemplate(path: String): Boolean =
    SupportedExtensions.exists(extension => path.endsWith(extension))

  override def mergeToFile(context: MergeContext, path: String): FileArtifact = {
    val f = templateContent.findFile(path)
    if (f.isEmpty)
      throw new IllegalArgumentException(s"Template '$path' not found")

    val fa = f.get
    val templateOutput = new StringWriter
    val filePath = mergeString(context, toInPlaceFilePath(path)).replace(":", "/")
    val mustache = mf.compile(fa.content)
    mustache.execute(templateOutput, context.map.asJava)
    StringFileArtifact(filePath, templateOutput.toString, fa.mode, fa.uniqueId)
  }

  override def mergeString(context: MergeContext, templateString: String): String = {
    val templateOutput = new StringWriter
    val mustache = mf.compile(templateString)
    mustache.execute(templateOutput, context.map.asJava)
    templateOutput.toString
  }

  private[mustache] def toInPlaceFilePath(path: String): String = {
    val extension = SupportedExtensions.find(extension => path.endsWith(s"_$extension"))
    if (extension.isDefined)
      path.dropRight(s"_${extension.get}".length)
    else
      path
  }
}
