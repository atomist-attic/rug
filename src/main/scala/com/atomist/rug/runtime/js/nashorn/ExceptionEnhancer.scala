package com.atomist.rug.runtime.js.nashorn

import java.util.Objects

import com.atlassian.sourcemap.SourceMapImpl
import com.atomist.rug.runtime.js.interop.{JavaScriptRuntimeException, RuntimeErrorInfo, SourceLanguageRuntimeException}
import com.atomist.source.ArtifactSource
import com.atomist.tree.content.text.LineInputPositionImpl
import jdk.nashorn.internal.objects.{NativeError, NativeTypeError}
import jdk.nashorn.internal.runtime.ECMAException

/**
  * Enhances exceptions caught within a JavaScript context that
  * we have all sources for
  */
private[nashorn] object ExceptionEnhancer {

  def enhanceIfPossible(rugAs: ArtifactSource, ecmaEx: ECMAException): Exception = {

    // Let this through. They probably threw it voluntarily.
    if (ecmaEx.thrown.isInstanceOf[NativeError])
      throw ecmaEx

    ecmaEx.getFileName match {
      case "<eval>" | null =>
        // Can't add much useful info
        ecmaEx
      case filePath =>
        val f = rugAs.findFile(filePath).getOrElse(
          throw new IllegalStateException(s"Cannot find file at path [$filePath] attempting to handle $ecmaEx")
        )

        val message: String =
          Objects.toString(ecmaEx.getThrown match {
          case nte: NativeTypeError =>
            nte.instMessage
          case x => x
        })

        val line = ecmaEx.getLineNumber
        val col = ecmaEx.getColumnNumber.max(1)
        // Nashorn can report -1, which breaks source maps
        val pos = LineInputPositionImpl(f.content, line, col)
        val jsri = RuntimeErrorInfo(message,
          f.path, pos, ecmaEx.getThrown)
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


