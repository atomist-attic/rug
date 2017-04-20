package com.atomist.rug.test.gherkin.handler

import com.atomist.graph.GraphNode
import com.atomist.project.archive.Rugs
import com.atomist.rug.RugNotFoundException
import com.atomist.rug.kind.core.{ProjectMutableView, RepoResolver}
import com.atomist.rug.runtime.CommandHandler
import com.atomist.rug.runtime.js.interop.{NashornMapBackedGraphNode, jsPathExpressionEngine, jsScalaHidingProxy}
import com.atomist.rug.runtime.js.{RugContext, SimpleContainerGraphNode}
import com.atomist.rug.spi.Handlers.Instruction.{Edit, Generate}
import com.atomist.rug.spi.Handlers.Plan
import com.atomist.rug.spi.TypeRegistry
import com.atomist.rug.test.gherkin.{Definitions, GherkinExecutionListener, ScenarioWorld}
import com.atomist.source.EmptyArtifactSource
import com.atomist.tree.{TreeMaterializer, TreeNode}
import jdk.nashorn.api.scripting.ScriptObjectMirror

/**
  * Superclass for Handler worlds. Handles plan capture and exposing to JavaScript
  */
abstract class AbstractHandlerScenarioWorld(definitions: Definitions, rugs: Option[Rugs], listeners: Seq[GherkinExecutionListener])
  extends ScenarioWorld(definitions, rugs) {

  // Handler name to plan
  private var recordedPlans = Map[String, Plan]()

  private val repoResolver = new MutableRepoResolver

  protected def createRugContext(tm: TreeMaterializer): RugContext =
    new FakeRugContext("team_id", tm, Some(repoResolver))

  private var rootContext: SimpleContainerGraphNode =
    SimpleContainerGraphNode.empty("root", TreeNode.Dynamic)

  private case class ProjectId(owner: String, repoName: String, sha: String)

  def emptyProject(name: String): ProjectMutableView = {
    new ProjectMutableView(originalBackingObject = EmptyArtifactSource(name = name))
  }

  def defineRepo(owner: String, name: String, branchOrSha: String, p: ProjectMutableView): Unit =
    repoResolver.defineRepo(owner, name, branchOrSha, p.currentBackingObject)

  /**
    * Return the editor with the given name or throw an exception
    */
  def commandHandler(name: String): CommandHandler = {
    rugs match {
      case Some(r) =>
        r.commandHandlers.find(e => e.name == name) match {
          case Some(e) => e
          case _ => throw new RugNotFoundException(
            s"CommandHandler with name '$name' can not be found in current context. Known CommandHandlers are [${r.commandHandlerNames.mkString(", ")}]")
        }
      case _ => throw new RugNotFoundException("No context provided")
    }
  }

  /**
    * Add a node to the root context
    */
  def addToRootContext(n: AnyRef): Unit = {
    val gn = NashornMapBackedGraphNode.toGraphNode(n).getOrElse(
      throw new IllegalArgumentException(s"$n is not a valid GraphNode")
    )
    rootContext = rootContext.addRelatedNode(gn)
  }

  protected def recordPlan(handlerName: String, plan: Plan): Unit = plan match {
    // TODO publish event indicating plan was recorded
    case _ =>
      recordedPlans = recordedPlans + (handlerName -> plan)
  }

  private def exposeToJavaScript(plan: Plan): AnyRef = plan.nativeObject match {
    case Some(som: ScriptObjectMirror) => som
    case _ => jsScalaHidingProxy(plan)
  }

  /**
    * Return a single plan or throw an exception if none was recorded
    */
  def requiredPlan: AnyRef = recordedPlans.values.toList match {
    case Nil =>
      throw new IllegalArgumentException("No plan was recorded")
    case plan :: Nil =>
      exposeToJavaScript(plan)
    case _ =>
      throw new IllegalArgumentException(s"Expected exactly one plan but found ${recordedPlans.size}")
  }

  /**
    * Return the plan or null if none was recorded
    */
  def plan: Any =
    recordedPlans.values.headOption.map(exposeToJavaScript).orNull

  /**
    * Return the plan recorded for this named handler, or null if not found
    */
  def planFor(handlerName: String): jsScalaHidingProxy =
    recordedPlans.get(handlerName)
      .map(jsScalaHidingProxy.apply(_))
      .orNull

  def planCount: Int = recordedPlans.size

  /**
    * Is the plan internally valid? Do the referenced handlers and other operations exist?
    */
  def planIsInternallyValid(): Boolean = {
    if (recordedPlans.isEmpty)
      throw new IllegalArgumentException("No plan was recorded")
    recordedPlans.values.forall(p => {
      !p.instructions.map(_.instruction).exists {
        case Edit(detail) =>
          val knownEditors: Seq[String] = rugs.map(_.editorNames).getOrElse(Nil)
          !knownEditors.contains(detail.name)
        case Generate(detail) =>
          val knownGenerators: Seq[String] = rugs.map(_.generatorNames).getOrElse(Nil)
          !knownGenerators.contains(detail.name)
        // TODO there are probably more cases here
        case _ => false
      }
    })
  }

  private class FakeRugContext(val teamId: String, _treeMaterializer: TreeMaterializer,
                               override val repoResolver: Option[RepoResolver])
    extends RugContext {

    override def typeRegistry: TypeRegistry = AbstractHandlerScenarioWorld.this.typeRegistry

    override val pathExpressionEngine = new jsPathExpressionEngine(this)

    override def treeMaterializer: TreeMaterializer = _treeMaterializer

    override def contextRoot(): GraphNode = rootContext

  }

}
