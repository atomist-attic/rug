package com.atomist.rug.runtime.js

import com.atomist.param.ParameterValues
import com.atomist.project.archive.{AtomistConfig, DefaultAtomistConfig, RugResolver}
import com.atomist.project.common.InvalidParametersException
import com.atomist.project.generate.ProjectGenerator
import com.atomist.rug.kind.core.{ProjectContext, ProjectMutableView}
import com.atomist.source.{ArtifactSource, EmptyArtifactSource}
import com.atomist.util.Timing._

/**
  * Find Generators in a Nashorn
  */
class JavaScriptProjectGeneratorFinder
  extends JavaScriptRugFinder[JavaScriptProjectGenerator] {

  /**
    * Is the supplied thing valid at all?
    */
  def isValid(obj: JavaScriptObject): Boolean = {
    obj.getMember("__kind") == "generator" &&
      obj.hasMember("populate") &&
      obj.getMember("populate").asInstanceOf[JavaScriptObject].isFunction
  }

  override def create(jsc: JavaScriptEngineContext, fnVar: JavaScriptObject, resolver: Option[RugResolver]): Option[JavaScriptProjectGenerator] = {
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
                                  jsc: JavaScriptEngineContext,
                                  jsVar: JavaScriptObject,
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
        case null | UNDEFINED =>
          pmv
        case js: JavaScriptObject if js.isFunction =>
          val userProject = jsc.invokeMember(jsVar,
            StartingPointFunction, Some(validated),
            pmv,
            pmv.context).asInstanceOf[ProjectMutableView]
          val userAs = raw + userProject.currentBackingObject
          new ProjectMutableView(rugAs, userAs, atomistConfig = DefaultAtomistConfig, Some(this), rugResolver = resolver)
        case x => throw new IllegalArgumentException(s"Don't know what to do with JavaScript member $x")
      }

      jsc.invokeMember(jsVar, "populate", Some(validated), projectToInvokePopulateWith)
      projectToInvokePopulateWith.currentBackingObject
    }
    logger.debug(s"$name.populate took ${elapsedMillis}ms")
    result
  }

}

object JavaScriptProjectGenerator {

  val StartingPointFunction = "startingPoint"
}
