package com.atomist.rug.runtime.js

import com.atomist.param.ParameterValues
import com.atomist.project.archive.{AtomistConfig, DefaultAtomistConfig, RugResolver}
import com.atomist.project.common.InvalidParametersException
import com.atomist.project.generate.ProjectGenerator
import com.atomist.rug.kind.core.ProjectMutableView
import com.atomist.source.{ArtifactSource, EmptyArtifactSource}
import com.atomist.util.Timing._
import jdk.nashorn.api.scripting.ScriptObjectMirror

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

  @throws(classOf[InvalidParametersException])
  override def generate(projectName: String, poa: ParameterValues): ArtifactSource = {
    val validated = addDefaultParameterValues(poa)
    validateParameters(validated)

    val (result, elapsedMillis) = time {
      val project = new EmptyArtifactSource(projectName) + startProject
      val pmv = new ProjectMutableView(rugAs, project, atomistConfig = DefaultAtomistConfig, Some(this), rugResolver = resolver)
      invokeMemberFunction(jsc, jsVar, "populate", Some(validated), wrapProject(pmv))
      pmv.currentBackingObject
    }
    logger.debug(s"$name.populate took ${elapsedMillis}ms")
    result
  }

}
