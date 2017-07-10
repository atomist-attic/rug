package com.atomist.rug.runtime.js.v8

import java.io.File
import java.nio.file.{Files, Paths}
import java.util.Objects

import com.atlassian.sourcemap.SourceMapImpl
import com.atomist.rug.runtime.js.interop.{JavaScriptRuntimeException, RuntimeErrorInfo, SourceLanguageRuntimeException}
import com.atomist.source.ArtifactSource
import com.atomist.tree.content.text.LineInputPositionImpl
import com.eclipsesource.v8.V8ScriptExecutionException
import org.apache.commons.io.FileUtils

/**
  * Enhances exceptions caught within a JavaScript context that
  * we have all sources for
  */
private[v8] object ExceptionEnhancer {

  def enhanceIfPossible(rugAs: ArtifactSource, ecmaEx: V8ScriptExecutionException): Exception = {

//    // Let this through. They probably threw it voluntarily.
    if (ecmaEx.getJSMessage.startsWith("Error:"))
      throw ecmaEx

    ecmaEx.getFileName match {
      case "<eval>" | null =>
        // Can't add much useful info
        ecmaEx
      case filePath =>

        val f = rugAs.findFile(filePath.substring(filePath.indexOf(".atomist/"))).getOrElse(
          throw new IllegalStateException(s"Cannot find file at path [$filePath] attempting to handle $ecmaEx")
        )

        val message: String = ecmaEx.getJSMessage

        val line = Math.max(ecmaEx.getLineNumber -1, 1) // V8 line numbers are 0 based
        val col = ecmaEx.getStartColumn
        val pos = LineInputPositionImpl(f.content, line, col)
        val jsri = RuntimeErrorInfo(message,
          f.path, pos, ecmaEx.getCause)
        findSourceMappedSourceInformation(rugAs, jsri) match {
          case Some(rti) => new SourceLanguageRuntimeException(jsri, rti)
          case None => new JavaScriptRuntimeException(jsri)
        }
    }
  }

  private def findSourceMappedSourceInformation(rugAs: ArtifactSource, jsri: RuntimeErrorInfo): Option[RuntimeErrorInfo] = {
    val sourceMapName = jsri.filePath + ".map"
    rugAs.findFile(sourceMapName).flatMap(map => {
      val sourceMap = new SourceMapImpl(map.content)
      val mapping = sourceMap.getMapping(jsri.pos.lineFrom1, jsri.pos.colFrom1)
      Option(mapping).flatMap(mapping => {
        val sourcePath = jsri.filePath.substring(0, jsri.filePath.lastIndexOf("/"))
        val sourceFileO = rugAs.findFile(s"$sourcePath/${mapping.getSourceFileName}")
        sourceFileO.map(sourceFile => {
          val lip = LineInputPositionImpl(sourceFile.content,
            mapping.getSourceLine + 1,
            mapping.getSourceLine)
          RuntimeErrorInfo(jsri.message, sourceFile.path, lip, jsri.detail)
        })
      })
    })
  }

}


