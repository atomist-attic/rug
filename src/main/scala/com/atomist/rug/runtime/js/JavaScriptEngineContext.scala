package com.atomist.rug.runtime.js

import com.atomist.param.ParameterValues
import com.atomist.source.{ArtifactSource, FileArtifact}

/**
  * A JavaScript engine implementation
  */
trait JavaScriptEngineContext {
  def evaluate(f: FileArtifact): Unit
  def members(): Seq[JavaScriptMember]
  def rugAs: ArtifactSource
  def invokeMember(jsVar: JavaScriptObject, member: String, params: Option[ParameterValues], args: Object*): Any
  def parseJson(json: String): JavaScriptObject
  def setMember(name: String, value: AnyRef): Unit
  def atomistContent(): ArtifactSource
}

/**
  * Information about a JavaScript var exposed in the project scripts
  *
  * @param key                name of the var
  * @param obj                interface for working with Var
  */
case class JavaScriptMember(key: String, obj: JavaScriptObject)
