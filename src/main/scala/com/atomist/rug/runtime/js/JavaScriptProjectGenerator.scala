package com.atomist.rug.runtime.js

import com.atomist.param.ParameterValues
import com.atomist.project.archive.{AtomistConfig, DefaultAtomistConfig, RugResolver}
import com.atomist.project.common.InvalidParametersException
import com.atomist.project.generate.ProjectGenerator
import com.atomist.rug.kind.core.{ProjectContext, ProjectMutableView}
import com.atomist.rug.runtime.js.interop.jsScalaHidingProxy
import com.atomist.source.{ArtifactSource, EmptyArtifactSource}
import com.atomist.util.Timing._
import jdk.nashorn.api.scripting.{JSObject, ScriptObjectMirror}
import jdk.nashorn.internal.runtime.ScriptRuntime

/**
  * Find Generators in a Nashorn
  */
class JavaScriptProjectGeneratorFinder
  extends JavaScriptRugFinder[JavaScriptProjectGenerator] {

  /**
    * Is the supplied thing valid at all?
    */
  def isValid(obj: ScriptObjectMirror): Boolean = {
    obj.getMember("__kind") == "generator" &&
      obj.hasMember("populate") &&
      obj.getMember("populate").asInstanceOf[ScriptObjectMirror].isFunction
  }

  override def create(jsc: JavaScriptContext, fnVar: ScriptObjectMirror, resolver: Option[RugResolver]): Option[JavaScriptProjectGenerator] = {
    val project: ArtifactSource = removeAtomistTemplateContent(jsc.rugAs)
    Some(new JavaScriptProjectGenerator(jsc, fnVar, jsc.rugAs, project, resolver))
  }

  private def removeAtomistTemplateContent(startingProject: ArtifactSource, atomistConfig: AtomistConfig = DefaultAtomistConfig): ArtifactSource = {
    startingProject.filter(d => !d.path.equals(atomistConfig.atomistRoot), f => true)
  }
}

/**
  * ProjectEditor implementation that invokes a JavaScript function. This will probably be the result of
  * TypeScript compilation, but need not be. Attempts to source metadata from annotations.
  */
class JavaScriptProjectGenerator(
                                  jsc: JavaScriptContext,
                                  jsVar: ScriptObjectMirror,
                                  rugAs: ArtifactSource,
                                  startProject: ArtifactSource,
                                  resolver: Option[RugResolver]
                                )
  extends JavaScriptProjectOperation(jsc, jsVar, rugAs)
    with ProjectGenerator {

  import JavaScriptProjectGenerator.StartingPointFunction

  @throws(classOf[InvalidParametersException])
  override def generate(projectName: String, pvs: ParameterValues, ctx: ProjectContext): ArtifactSource = {
    val validated = addDefaultParameterValues(pvs)
    validateParameters(validated)

    val raw = new EmptyArtifactSource(projectName)
    val project = raw + startProject
    val pmv = new ProjectMutableView(rugAs, project, atomistConfig = DefaultAtomistConfig, Some(this),
      rugResolver = resolver,
      ctx = ctx)

    val (result, elapsedMillis) = time {
      // If the user has provided this method, call it to get the project starting point
      val projectToInvokePopulateWith = jsVar.getMember(StartingPointFunction) match {
        case null | ScriptRuntime.UNDEFINED =>
          pmv
        case js: JSObject if js.isFunction =>
          val userProject = invokeMemberFunction(jsc, jsVar,
            StartingPointFunction, None,
            wrapProject(pmv), jsScalaHidingProxy(pmv.context)).asInstanceOf[ProjectMutableView]
          val userAs = raw + userProject.currentBackingObject
          new ProjectMutableView(rugAs, userAs, atomistConfig = DefaultAtomistConfig, Some(this), rugResolver = resolver)
        case x => throw new IllegalArgumentException(s"Don't know what to do with JavaScript member $x")
      }

      invokeMemberFunction(jsc, jsVar, "populate", Some(validated), wrapProject(projectToInvokePopulateWith))
      projectToInvokePopulateWith.currentBackingObject
    }
    logger.debug(s"$name.populate took ${elapsedMillis}ms")
    result
  }

}

object JavaScriptProjectGenerator {

  val StartingPointFunction = "startingPoint"
}
