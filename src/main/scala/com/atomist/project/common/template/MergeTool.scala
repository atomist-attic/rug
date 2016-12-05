package com.atomist.project.common.template

import java.util.{Map => JMap}

import com.atomist.param.ParameterValues
import com.atomist.source.{ArtifactSource, FileArtifact, SimpleFileEditor}
import com.atomist.util.BinaryDecider

import scala.collection.JavaConversions._

/**
  * Simple abstraction for a template engine.
  */
trait MergeTool {

  def isTemplate(f: FileArtifact): Boolean = isTemplate(f.path)

  def isTemplate(path: String): Boolean

  /**
    * Merge the given file, including working out a new file name if it contains template syntax.
    *
    * @param context Merge context
    * @param templatePath path within the backing ArtifactSource or
    * other source of templates
    * @return new file, which may have a different name
    */
  def mergeToFile(context: MergeContext, templatePath: String): FileArtifact

  /**
    * Process the given files, including handling as templates and renaming if necessary.
    *
    * @param context Merge context
    * @param in files to process
    * @return output files
    */
  def processTemplateFiles(context: MergeContext, in: Seq[FileArtifact]): Seq[FileArtifact] =
    in.map(f => mergeFile(context, f))

  private def mergeFile(context: MergeContext, f: FileArtifact) = f match {
    case `f` if isTemplate(f) =>
      mergeToFile(context, f.path)
    case `f` => f
  }

  /**
    * Process the given ArtifactSource containing template and static content.
    *
    * @param context merge context
    * @param in ArtifactSource we're processing
    * @return output ArtifactSource
    */
  def processTemplateFiles(context: MergeContext, in: ArtifactSource): ArtifactSource =
    in ✎ SimpleFileEditor(f => !BinaryDecider.isBinaryContent(f.content), f => mergeFile(context, f))

  /**
    * Merge the given template string.
    *
    * @param context merge context
    * @param templateString string that is itself a template
    * @return output string
    */
  def mergeString(context: MergeContext, templateString: String): String
}

case class MergeContext(map: Map[String, Any]) {

  def this(m: JMap[String, Object]) = this(m.toMap)

  def this(pd: ParameterValues) =
    this(pd.parameterValues.map(pv => (pv.getName, pv.getValue)).toMap)
}
