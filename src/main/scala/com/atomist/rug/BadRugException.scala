package com.atomist.rug

import javax.script.ScriptException

import com.atomist.rug.parser.Annotation
import com.atomist.util.scalaparsing.ErrorInfo
import com.atomist.source.FileArtifact

abstract class BadRugException(msg: String, rootCause: Throwable = null)
  extends Exception(msg, rootCause)

class BadRugPackagingException(msg: String, val f: FileArtifact, val progs: Seq[RugProgram])
  extends BadRugException(f.path + ": " + msg)

/**
  * Parser exception.
  */
class BadRugSyntaxException(
                             val info: ErrorInfo,
                             rootCause: Throwable = null)
  extends BadRugException(info.message, rootCause) {

  override def getMessage: String = info.toString

  override def toString: String =
    info.toString

}

class InvalidRugAnnotationValueException(op: String, val ann: Annotation)
  extends BadRugException(op + ": " + s"Invalid annotation: $ann")

class UndefinedRugTypeException(op: String, msg: String, val typeName: String)
  extends BadRugException(op + ": " + msg)

class UndefinedRugFunctionsException(val op: String, msg: String, val functionNames: Set[String])
  extends BadRugException(op + ": " + msg)

class UndefinedRugIdentifiersException(val op: String, msg: String, val identifierNames: Seq[String])
  extends BadRugException(op + ": " + msg)

class UndefinedTypeException(val op: String, msg: String, val moduleName: String)
  extends BadRugException(op + ": " + msg)

class UndefinedProjectNameParameterException(val op: String, msg: String, val moduleName: String)
  extends BadRugException(op + ": " + msg)

class RugRuntimeException(val op: String, msg: String, rootCause: Throwable = null)
  extends RuntimeException((if (op == null) "<unknown>" else op) + ": " + msg, rootCause)

class RugReferenceException(op: String, msg: String, rootCause: Throwable = null)
  extends RugRuntimeException(op, msg, rootCause)

class UndefinedRugUsesException(op: String, msg: String, val moduleNames: Seq[String])
  extends RugReferenceException(op, msg, null)

class UnusedUsesException(op: String, msg: String, val unusedUses: Seq[String])
  extends RugReferenceException(op, msg, null)

class InvalidRugUsesException(op: String, msg: String, val uses: String)
  extends RugReferenceException(op, msg, null)

class InvalidRugParameterPatternException(msg: String)
   extends BadRugException(msg)

class InvalidRugParameterDefaultValue(msg: String)
  extends BadRugException(msg)

class RugJavaScriptException(msg: String, rootCause: ScriptException)
   extends BadRugException(msg,rootCause)

class InvalidRugTestScenarioName(msg: String)
  extends BadRugException(msg)

class InvalidHandlerResultException(msg: String)
  extends BadRugException(msg)
