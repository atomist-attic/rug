package com.atomist.rug

import javax.script.ScriptException

import com.atomist.project.edit.ProjectEditor
import com.atomist.rug.runtime.Rug
import com.atomist.rug.runtime.RugScopes.Scope
import com.atomist.util.scalaparsing.ErrorInfo

abstract class BadRugException(msg: String, rootCause: Throwable = null)
  extends Exception(msg, rootCause)

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

class MissingSecretException(msg: String)
  extends BadRugException(msg)

class InvalidSecretException(msg: String)
  extends BadRugException(msg)

class RugNotFoundException(msg: String)
  extends BadRugException(msg)

class BadPlanException(msg: String, rootCause: Throwable = null)
  extends BadRugException(msg, rootCause)

class MissingRugException(msg: String)
  extends BadRugException(msg)

class DuplicateRugException(msg: String, knownRugs: Seq[Rug])
   extends BadRugException(msg)

class InvalidTestDescriptorException(msg: String)
  extends BadRugException(msg)


class InvalidRugScopeException(msg: String, validScopes: Seq[Scope])
  extends BadRugException(msg)

class EditorNotFoundException(msg: String)
  extends BadRugException(msg) {
  def this(requestedEditorName: String, knownRugs: Seq[Rug]) = {
    this(EditorNotFoundExceptionStr(requestedEditorName, knownRugs))
  }
}

/**
  * Generate a an exception string containing all known editors.
  */
private object EditorNotFoundExceptionStr {
  def apply(requestedEditorName: String, knownRugs: Seq[Rug]): String = {
    s"""|Could not find editor [$requestedEditorName]. Known editors:
        |[${knownRugs.collect{case o: ProjectEditor => o}.map(p => p.name).mkString(",")}]""".stripMargin
  }
}

class BadRugFunctionResponseException(msg: String)
  extends BadRugException(msg)

class InvalidHandlerException(msg: String)
  extends BadRugException(msg)
