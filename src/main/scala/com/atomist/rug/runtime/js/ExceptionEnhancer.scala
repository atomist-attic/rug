package com.atomist.rug.runtime.js

import java.util.Objects

import com.atlassian.sourcemap.SourceMapImpl
import com.atomist.source.ArtifactSource
import com.atomist.tree.content.text.{LineInputPosition, LineInputPositionImpl}
import jdk.nashorn.internal.objects.NativeTypeError
import jdk.nashorn.internal.runtime.ECMAException


case class RuntimeErrorInfo(message: String,
                            filePath: String,
                            pos: LineInputPosition,
                            detail: Object) {

  override def toString: String =
    s"[${message}] at ${positionInfo}"

  def positionInfo: String =
    s"${filePath}:${pos.lineFrom1}/${pos.colFrom1}\n${pos.show}"

}


/**
  * Enhances exceptions caught within a JavaScript context that
  * we have all sources for
  */
object ExceptionEnhancer {

  def enhanceIfPossible(rugAs: ArtifactSource, ecmaEx: ECMAException): Exception = {
    //println(s"${ecmaEx.getFileName} at ${ecmaEx.getLineNumber}/${ecmaEx.getColumnNumber}, thrown = ${ecmaEx.getThrown}, ${ecmaEx.getCause}, ${ecmaEx.getEcmaError}")
    //e.getThrown.asInstanceOf[Throwable].printStackTrace()
    ecmaEx.getFileName match {
      case "<eval>" | null =>
        // Can't add much useful info
        ecmaEx
      case filePath =>
        val f = rugAs.findFile(filePath).getOrElse(
          throw new IllegalStateException(s"Cannot find file at path [$filePath] attempting to handle ${ecmaEx}")
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
    rugAs.findFile(sourceMapName).map(map => {
      val sourceMap = new SourceMapImpl(map.content)
      val mapping = sourceMap.getMapping(jsri.pos.lineFrom1, jsri.pos.colFrom1)
      val sourcePath = jsri.filePath.substring(0, jsri.filePath.lastIndexOf("/"))
      val sourceFile = rugAs.findFile(s"$sourcePath/${mapping.getSourceFileName}").getOrElse(
        throw new IllegalArgumentException(s"Can't find source file [${mapping.getSourceFileName}]")
      )
      val lip = LineInputPositionImpl(sourceFile.content,
        mapping.getSourceLine + 1,
        mapping.getSourceLine)
      RuntimeErrorInfo(jsri.message, sourceFile.path, lip, jsri.detail)
    })
  }

}


/**
  * Thrown when there's an exception in the JavaScript or TypeScript runtime
  */
class JavaScriptRuntimeException(
                                  val jsRuntimeErrorInfo: RuntimeErrorInfo,
                                  message: String)
  extends Exception(message) {

  def this(jsri: RuntimeErrorInfo) =
    this(jsri, jsri.toString)

  // Supress stack trace
  override def fillInStackTrace(): Throwable = this
}


/**
  * Has information both about the underlying JavaScript error and the original one,
  * in a language such as TypeScript
  * @param jsRuntimeErrorInfo JavaScript location
  * @param sourceLangRuntimeErrorInfo original language location
  */
class SourceLanguageRuntimeException(jsRuntimeErrorInfo: RuntimeErrorInfo,
                                     val sourceLangRuntimeErrorInfo: RuntimeErrorInfo
                                    )
  extends JavaScriptRuntimeException(jsRuntimeErrorInfo,
    sourceLangRuntimeErrorInfo.toString + "\n--via generated code--\n" +
      jsRuntimeErrorInfo.positionInfo) {

}