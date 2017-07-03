package com.atomist.rug.runtime.js

import com.atomist.param.{Parameter, Tag}
import com.atomist.project.ProjectOperation
import com.atomist.rug.kind.DefaultTypeRegistry
import com.atomist.rug.kind.core.ProjectMutableView
import com.atomist.rug.runtime.js.nashorn.jsSafeCommittingProxy
import com.atomist.source.ArtifactSource
import com.typesafe.scalalogging.LazyLogging

/**
  * Superclass for all operations that delegate to JavaScript.
  *
  * @param _jsc   JavaScript context
  * @param _jsVar var reference in Nashorn
  * @param rugAs  backing artifact source for the Rug archive
  */
abstract class JavaScriptProjectOperation(
                                           _jsc: JavaScriptEngineContext,
                                           _jsVar: JavaScriptObject,
                                           rugAs: ArtifactSource
                                         )
  extends ProjectOperation
    with LazyLogging
    with JavaScriptUtils {

  /** Needed by BDD testing support */
  val jsVar: JavaScriptObject = _jsVar

  // Visible for test
  private[js] val jsc = _jsc

  override val tags: Seq[Tag] = tags(jsVar)

  override val parameters: Seq[Parameter] = parameters(jsVar)

  override val name: String = name(jsVar)

  override val description: String = description(jsVar)
}
