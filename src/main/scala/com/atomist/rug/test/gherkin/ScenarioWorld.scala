package com.atomist.rug.test.gherkin

import com.atomist.param.{ParameterValues, SimpleParameterValues}
import com.atomist.project.archive.Rugs
import com.atomist.project.common.InvalidParametersException
import com.atomist.rug.kind.DefaultTypeRegistry
import com.atomist.rug.kind.core.ProjectMutableView
import com.atomist.rug.runtime.js.interop.NashornUtils
import com.atomist.rug.spi.{TypeRegistry, Typed, UsageSpecificTypeRegistry}
import com.atomist.rug.ts.{CortexTypeGenerator, DefaultTypeGeneratorConfig}
import com.atomist.source.git.{GitArtifactSourceIdentifier, GitRepositoryCloner}
import jdk.nashorn.api.scripting.ScriptObjectMirror

object ScenarioWorld {

  lazy val ExtendedTypes: TypeRegistry = CortexTypeGenerator.extendedTypes(DefaultTypeGeneratorConfig.CortexJson)

}

/**
  * Standard world for a scenario that lets us add bindings
  * and subclasses attach further state and helper methods.
  */
abstract class ScenarioWorld(val definitions: Definitions, rugs: Option[Rugs], config: GherkinRunnerConfig) {

  private var bindings: Map[String, Object] = Map()

  private var abortedBy: Option[String] = None

  private var ipe: InvalidParametersException = _

  private var tr: TypeRegistry = DefaultTypeRegistry + ScenarioWorld.ExtendedTypes

  def typeRegistry: TypeRegistry = tr

  def registerType(t: Typed): Unit = {
    this.tr = new UsageSpecificTypeRegistry(this.tr, Seq(t))
  }

  /**
    * Target for each test. Defaults to the world. First parameter to step functions
    */
  def target: AnyRef = this

  /**
    * Was the scenario run aborted?
    */
  def aborted: Boolean = abortedBy.isDefined

  def abortMessage: String = abortedBy.getOrElse("NOT aborted")

  /**
    * Abort the scenario run, for example, because a given threw an exception.
    */
  def abort(msg: String): Unit = {
    abortedBy = Some(msg)
  }

  def put(key: String, value: Object): Unit = {
    bindings = bindings + (key -> value)
  }

  def get(key: String): Object =
    bindings.get(key).orNull

  def clear(key: String): Unit =
    bindings = bindings - key

  /**
    * Invalid parameters exception that aborted execution, or null
    */
  def invalidParameters: InvalidParametersException = ipe

  def logInvalidParameters(ipe: InvalidParametersException): Unit = {
    this.ipe = ipe
  }

  protected def parameters(params: Any): ParameterValues = {
    val m: Map[String, Object] = params match {
      case som: ScriptObjectMirror =>
        // The user has created a new JavaScript object, as in { foo: "bar" },
        // to pass up as an argument to the invoked editor. Extract its properties
        NashornUtils.extractProperties(som)
    }
    SimpleParameterValues(m)
  }

  protected case class RepoIdentification(owner: String, name: String, branch: Option[String], sha: Option[String])

  def cloneRepo(cloneInfo: AnyRef): ProjectMutableView = {
    val rid = extractRepoId(cloneInfo)
    val cloner = new GitRepositoryCloner(oAuthToken = config.oAuthToken.getOrElse(""))
    val as = cloner.clone(rid.name, rid.owner, rid.branch, rid.sha)
    new ProjectMutableView(as)
  }

  import NashornUtils._

  protected def extractRepoId(o: AnyRef): RepoIdentification = o match {
    case som: ScriptObjectMirror =>
      val owner = stringProperty(som, "owner")
      val name = stringProperty(som, "name")
      val branch = stringProperty(som, "branch")
      val sha = stringProperty(som, "sha")
      RepoIdentification(owner, name, Option(branch), Option(sha))
    case x =>
      throw new IllegalArgumentException(s"Required JavaScript object repo ID, not $x")
  }

}
