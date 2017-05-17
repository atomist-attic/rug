package com.atomist.rug.kind.java

import java.io.InputStreamReader
import java.util.{List => JList}

import com.atomist.source.{ArtifactSourceAccessException, FileArtifact}
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.{JavaParser, ParseException}
import com.typesafe.scalalogging.LazyLogging

case class FileAndCompilationUnit(file: FileArtifact, compilationUnit: CompilationUnit)

import com.atomist.util.Utils.withCloseable

import scala.collection.JavaConverters._

/**
  * Extract JavaParser CompilationUnits from Artifact Source.
  */
object GitHubJavaParserExtractor extends Function[JList[FileArtifact], Seq[FileAndCompilationUnit]] with LazyLogging {

  override def apply(javaFiles: JList[FileArtifact]): Seq[FileAndCompilationUnit] = {
    javaFiles.asScala.map(f => {
      logger.debug(s"Looking at Java artifact $f using $this")
      withCloseable(f.inputStream())(is =>
        withCloseable(new InputStreamReader(is))(reader => {
          try {
            FileAndCompilationUnit(f, JavaParser.parse(reader))
          } catch {
            case pex: ParseException => throw new ArtifactSourceAccessException(s"Parsing error in ${f.path},content was\n${f.content}", pex)
          }
        })
      )
    })
  }
}
