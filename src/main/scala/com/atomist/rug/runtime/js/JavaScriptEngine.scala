package com.atomist.rug.runtime.js

import com.atomist.param.ParameterValues
import com.atomist.project.archive.AtomistConfig
import com.atomist.source.{ArtifactSource, FileArtifact}

/**
  * A JavaScript engine implementation
  */
trait JavaScriptEngine {
  def evaluate(f: FileArtifact): Unit
  def atomistConfig: AtomistConfig
  def members(): Seq[JavaScriptMember]
  def rugAs: ArtifactSource
  def invokeMember(jsVar: JavaScriptObject, member: String, params: Option[ParameterValues], args: Object*): AnyRef
  def parseJson(json: String): JavaScriptObject
  def setMember(name: String, value: AnyRef): Unit
  def atomistContent(): ArtifactSource
  def eval(script: String): AnyRef
}

/**
  * Information about a JavaScript var exposed in the project scripts
  *
  * @param key                name of the var
  * @param obj                interface for working with Var
  */
case class JavaScriptMember(key: String, obj: JavaScriptObject)
