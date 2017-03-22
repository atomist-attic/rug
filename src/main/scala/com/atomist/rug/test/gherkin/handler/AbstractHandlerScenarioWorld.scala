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
import com.atomist.tree.TreeMaterializer

/**
  * Superclass for Handler worlds. Handles plan capture and exposing to JavaScript
  */
abstract class AbstractHandlerScenarioWorld(definitions: Definitions, rugs: Option[Rugs])
  extends ScenarioWorld(definitions, rugs) {

  private var planOption: Option[Plan] = None

  protected def createRugContext(tm: TreeMaterializer): RugContext =
    new FakeRugContext("team_id", tm)

  private var rootContext: SimpleContainerGraphNode = SimpleContainerGraphNode("root")

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

  protected def recordPlan(plan: Option[Plan]): Unit = {
    //println(s"Recorded plan option $plan")
    this.planOption = plan
  }

  /**
    * Return the plan or throw an exception if none was recorded
    */
  def requiredPlan: jsScalaHidingProxy = {
    //println(s"Contents of recorded plan: $planOption")
    planOption.map(jsScalaHidingProxy.apply(_)).getOrElse(throw new IllegalArgumentException("No plan was recorded"))
  }

  /**
    * Return the plan or null if none was recorded
    */
  def plan: jsScalaHidingProxy = {
    planOption.map(jsScalaHidingProxy.apply(_)).orNull
  }

  /**
    * Return the message or null if none was recorded
    */
  def message: jsScalaHidingProxy = {
    planOption match {
      case Some(p) => p.messages.map(jsScalaHidingProxy.apply(_)).head
      case _ => null
    }
  }

  /**
    * Is the plan internally valid? Do the referenced handlers and other operations exist?
    */
  def planIsInternallyValid(): Boolean = {
    val p = planOption.getOrElse(throw new IllegalArgumentException("No plan was recorded"))
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
  }

  private class FakeRugContext(val teamId: String, _treeMaterializer: TreeMaterializer) extends RugContext {

    override def typeRegistry: TypeRegistry = AbstractHandlerScenarioWorld.this.typeRegistry

    override val pathExpressionEngine = new jsPathExpressionEngine(this, typeRegistry = typeRegistry)

    override def treeMaterializer: TreeMaterializer = _treeMaterializer

    override def contextRoot(): GraphNode = rootContext

  }

}
