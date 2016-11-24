package com.atomist.rug.kind.java

import com.atomist.project.ProjectOperationArguments
import com.atomist.project.common.JavaTag
import com.atomist.project.edit._
import com.atomist.project.edit.common.ProjectEditorSupport
import com.atomist.rug.kind.java.support.{IsJavaProject, JavaConstants, JavaFilesExtractor}
import com.atomist.source.{ArtifactSource, FileArtifact, SimpleFileEditor, StringFileArtifact}
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.{CompilationUnit, ImportDeclaration}
import com.typesafe.scalalogging.LazyLogging

import scala.collection.JavaConversions._

/**
  * Support for in-place editing of Java files using GitHubJavaParser.
  */
abstract class JavaParserProjectEditor(val name: String,
                                       javaSourcePath: String = JavaConstants.DefaultBaseSourcePath)
  extends ProjectEditorSupport
    with LazyLogging {

  addTag(JavaTag)

  private val extractJavaFiles: ArtifactSource => Seq[FileArtifact] =
    a => JavaFilesExtractor(a / javaSourcePath)

  override def impacts(): Set[Impact] = Set(CodeImpact)

  final override def applicability(as: ArtifactSource): Applicability = {
    Applicability(IsJavaProject(as), "IsJava")
  }

  protected final override def modifyInternal(as: ArtifactSource, poa: ProjectOperationArguments): ModificationAttempt = {
    val javaFiles = extractJavaFiles(as)

    val filesAndCompilationUnits = GitHubJavaParserExtractor(javaFiles)

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

      SuccessfulModification(result, impacts(), s"$name success")
    }
  }

  /**
    * Returns an Option of a modified compilation unit.
    *
    * @param cu the compilation unit
    * @return modified compilation unit if a change was made
    */
  protected def maybeModifyCompilationUnit(cu: CompilationUnit, poa: ProjectOperationArguments): Option[CompilationUnit]
}

/**
  * Convenience methods for modifying JavaParser objects.
  */
object JavaParserProjectEditor {

  def addImportsIfNeeded(fqns: Seq[String], cu: CompilationUnit): Unit = {
    fqns.foreach(fqn => {
      val importDefinition = cu.getImports.find(imp => imp.toString contains fqn)
      if (importDefinition.isEmpty) {
        cu.getImports.add(new ImportDeclaration(new NameExpr(fqn), false, false))
      }
    })
  }
}
