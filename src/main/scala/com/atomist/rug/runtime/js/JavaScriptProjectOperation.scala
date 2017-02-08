package com.atomist.rug.runtime.js

import com.atomist.project.common.support.ProjectOperationParameterSupport
import com.atomist.project.ProjectOperation
import com.atomist.rug.kind.core.ProjectMutableView
import com.atomist.rug.runtime.js.interop.jsSafeCommittingProxy
import com.atomist.rug.runtime.rugdsl.ContextAwareProjectOperation
import com.atomist.source.ArtifactSource
import com.typesafe.scalalogging.LazyLogging
import jdk.nashorn.api.scripting.ScriptObjectMirror

/**
  * Superclass for all operations that delegate to JavaScript.
  *
  * @param _jsc       JavaScript context
  * @param _jsVar     var reference in Nashorn
  * @param rugAs     backing artifact source for the Rug archive
  */
abstract class JavaScriptProjectOperation(
                                                   _jsc: JavaScriptContext,
                                                   _jsVar: ScriptObjectMirror,
                                                   rugAs: ArtifactSource
                                                 )
  extends ProjectOperationParameterSupport
    with ContextAwareProjectOperation
    with LazyLogging
    with JavaScriptUtils{

  //visible for test
  private[js] val jsVar = _jsVar
  private[js] val jsc = _jsc

  tags(jsVar, Seq("__tags", "tags")).foreach(t => addTag(t))

  parameters(jsVar, Seq("__parameters", "parameters")).foreach(p => addParameter(p))

  private var _context: Seq[ProjectOperation] = Nil

  override def setContext(ctx: Seq[ProjectOperation]): Unit = {
    _context = ctx
  }

  protected def context: Seq[ProjectOperation] = {
    _context
  }

  override def name: String = getMember(jsVar, Seq("__name", "name")).get.asInstanceOf[String]

  override def description: String = getMember(jsVar, Seq("__description", "description")).get.asInstanceOf[String]

  /**
    * Convenient class allowing subclasses to wrap projects in a safe, updating proxy
    *
    * @param pmv project to wrap
    * @return proxy TypeScript callers can use
    */
  protected def wrapProject(pmv: ProjectMutableView): jsSafeCommittingProxy = {
    new jsSafeCommittingProxy(pmv)
  }
}
