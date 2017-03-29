package com.atomist.rug.test.gherkin.handler

import com.atomist.graph.GraphNode
import com.atomist.project.archive.Rugs
import com.atomist.rug.RugNotFoundException
import com.atomist.rug.runtime.CommandHandler
import com.atomist.rug.runtime.js.interop.{NashornMapBackedGraphNode, jsPathExpressionEngine, jsScalaHidingProxy}
import com.atomist.rug.runtime.js.{RugContext, SimpleContainerGraphNode}
import com.atomist.rug.spi.Handlers.Instruction.{Command, Edit, Generate, Review}
import com.atomist.rug.spi.Handlers.Plan
import com.atomist.rug.spi.TypeRegistry
import com.atomist.rug.test.gherkin.{Definitions, ScenarioWorld}
import com.atomist.tree.{TreeMaterializer, TreeNode}

/**
  * Superclass for Handler worlds. Handles plan capture and exposing to JavaScript
  */
abstract class AbstractHandlerScenarioWorld(definitions: Definitions, rugs: Option[Rugs])
  extends ScenarioWorld(definitions, rugs) {

  // Handler name to plan
  private var recordedPlans = Map[String, Plan]()

  protected def createRugContext(tm: TreeMaterializer): RugContext =
    new FakeRugContext("team_id", tm)

  private var rootContext: SimpleContainerGraphNode =
    SimpleContainerGraphNode.empty("root", TreeNode.Dynamic)

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

  protected def recordPlan(handlerName: String, plan: Plan): Unit = {
    //println(s"Recorded plan $plan")
    recordedPlans = recordedPlans + (handlerName -> plan)
  }

  /**
    * Return a single plan or throw an exception if none was recorded
    */
  def requiredPlan: jsScalaHidingProxy = recordedPlans.values.toList match {
    case Nil =>
      throw new IllegalArgumentException("No plan was recorded")
    case plan :: Nil =>
      //println(s"Contents of recorded plan: $planOption")
      jsScalaHidingProxy(plan)
    case _ =>
      throw new IllegalArgumentException(s"Expected exactly one plan but found ${recordedPlans.size}")
  }

  /**
    * Return the plan or null if none was recorded
    */
  def plan: jsScalaHidingProxy =
    recordedPlans.values.headOption.map(jsScalaHidingProxy.apply(_)).orNull

  /**
    * Return the plan recorded for this named handler, or null if not found
    */
  def planFor(handlerName: String): jsScalaHidingProxy =
    recordedPlans.get(handlerName)
      .map(jsScalaHidingProxy.apply(_))
      .orNull

  def planCount: Int = recordedPlans.size

  /**
    * Return the message or null if none was recorded
    */
  def message: jsScalaHidingProxy = {
    recordedPlans.values.toList match {
      case List(p) => p.messages.map(jsScalaHidingProxy.apply(_)).head
      case _ => null
    }
  }

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
        case Review(detail) =>
          val knownReviewers: Seq[String] = rugs.map(_.reviewerNames).getOrElse(Nil)
          !knownReviewers.contains(detail.name)
        case Command(detail) =>
          val knownCommandHandlers: Seq[String] = rugs.map(_.commandHandlerNames).getOrElse(Nil)
          !knownCommandHandlers.contains(detail.name)
        // TODO there are probably more cases here
        case _ => false
      }
    })
  }

  private class FakeRugContext(val teamId: String, _treeMaterializer: TreeMaterializer) extends RugContext {

    override def typeRegistry: TypeRegistry = AbstractHandlerScenarioWorld.this.typeRegistry

    override val pathExpressionEngine = new jsPathExpressionEngine(this, typeRegistry = typeRegistry)

    override def treeMaterializer: TreeMaterializer = _treeMaterializer

    override def contextRoot(): GraphNode = rootContext

  }

}
