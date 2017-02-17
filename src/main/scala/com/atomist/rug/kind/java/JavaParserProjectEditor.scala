package com.atomist.rug.kind.java

import com.atomist.param.ParameterValues
import com.atomist.project.common.JavaTag
import com.atomist.project.edit.{ProjectEditorSupport, _}
import com.atomist.rug.kind.java.support.{JavaAssertions, JavaFilesExtractor}
import com.atomist.source.{ArtifactSource, FileArtifact, SimpleFileEditor, StringFileArtifact}
import com.atomist.util.lang.JavaConstants
import com.github.javaparser.ast.CompilationUnit
import com.typesafe.scalalogging.LazyLogging

import scala.collection.JavaConverters._

/**
  * Support for in-place editing of Java files using GitHubJavaParser.
  */
abstract class JavaParserProjectEditor(val name: String,
                                       javaSourcePath: String = JavaConstants.DefaultBaseSourcePath)
  extends ProjectEditorSupport
    with LazyLogging {

  addTag(JavaTag)

  private val extractJavaFiles: ArtifactSource => Seq[FileArtifact] =
    a => JavaFilesExtractor(a / javaSourcePath).asScala

  final override def applicability(as: ArtifactSource): Applicability = {
    Applicability(JavaAssertions.isJava(as), "IsJava")
  }

  protected final override def modifyInternal(as: ArtifactSource, poa: ParameterValues): ModificationAttempt = {
    val javaFiles = extractJavaFiles(as)

    val filesAndCompilationUnits = GitHubJavaParserExtractor(javaFiles.asJava)

    val modifiedFiles: Seq[FileArtifact] =
      for {
        facu <- filesAndCompilationUnits
        modifiedCu <- maybeModifyCompilationUnit(facu.compilationUnit, poa)
      } yield {
        val f = StringFileArtifact(javaSourcePath + "/" + facu.file.path, modifiedCu.toString)
        logger.debug(s"Modified file: ${f.path}\n${f.content}\n")
        f
      }

    if (modifiedFiles.isEmpty)
      NoModificationNeeded(s"$name: No files modified")
    else {
      modifiedFiles.filter(_.name.endsWith("java")).foreach(f => logger.debug(s"${f.path}\n${f.content}\n"))

      val fe = SimpleFileEditor(f => modifiedFiles.exists(mf => mf.path equals f.path),
        f => modifiedFiles.find(mf => mf.path equals f.path).getOrElse(f))
      val result = as ✎ fe

      result.allFiles.filter(_.name.endsWith("java")).foreach(f => logger.debug(s"${f.path}\n${f.content}\n"))
      SuccessfulModification(result)
    }
  }

  /**
    * Returns an Option of a modified compilation unit.
    *
    * @param cu the compilation unit
    * @return modified compilation unit if a change was made
    */
  protected def maybeModifyCompilationUnit(cu: CompilationUnit, poa: ParameterValues): Option[CompilationUnit]
}
