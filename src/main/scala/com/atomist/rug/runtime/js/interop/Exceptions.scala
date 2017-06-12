package com.atomist.rug.runtime.js.interop

import com.atomist.tree.content.text.LineInputPosition


case class RuntimeErrorInfo(message: String,
                            filePath: String,
                            pos: LineInputPosition,
                            detail: Object) {

  override def toString: String =
    s"[$message] at $positionInfo"

  def positionInfo: String =
    s"$filePath:${pos.lineFrom1}/${pos.colFrom1}\n${pos.show}"

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
