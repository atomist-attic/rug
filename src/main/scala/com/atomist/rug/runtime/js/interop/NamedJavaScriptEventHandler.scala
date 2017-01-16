package com.atomist.rug.runtime.js.interop

import com.atomist.event.{HandlerContext, SystemEvent}
import com.atomist.param.Tag
import com.atomist.rug.RugRuntimeException
import com.atomist.rug.kind.DefaultTypeRegistry
import com.atomist.rug.kind.service._
import com.atomist.rug.runtime.js.JavaScriptEventHandler
import com.atomist.source.ArtifactSource
import com.atomist.tree.TreeNode
import com.atomist.tree.content.text.SimpleMutableContainerTreeNode
import jdk.nashorn.api.scripting.ScriptObjectMirror

import scala.collection.JavaConverters._

/**
  * Like super, except that we require a proper name, description, tags etc.
  * and we wrap the match in an Event
  */
class NamedJavaScriptEventHandler(pathExpressionStr: String,
                                  handlerFunction: ScriptObjectMirror,
                                  thiz: ScriptObjectMirror,
                                  rugAs: ArtifactSource,
                                  ctx: JavaScriptHandlerContext,
                                  _name: String,
                                  _description: String,
                                  _tags: Seq[Tag] = Nil)
  extends JavaScriptEventHandler(pathExpressionStr, handlerFunction, rugAs, ctx.treeMaterializer, ctx.pathExpressionEngine) {

  override def name: String = _name

  override def tags: Seq[Tag] = _tags

  override def description: String = _description

  override def handle(e: SystemEvent, s2: ServiceSource): Unit = {
    val smv = new ServicesMutableView(rugAs, s2)
    val handlerContext = HandlerContext(smv)
    val np = nodePreparer(handlerContext)

    val targetNode = ctx.treeMaterializer.rootNodeFor(e, pathExpression)
    // Put a new artificial root above to make expression work
    val root = new SimpleMutableContainerTreeNode("root", Seq(targetNode), null, null)
    ctx.pathExpressionEngine.ee.evaluate(root, pathExpression, DefaultTypeRegistry, Some(np)) match {
      case Right(Nil) =>
      case Right(matches) =>
        val cm = ContextMatch(
          targetNode,
          ctx.pathExpressionEngine.wrap(matches),
          s2,
          teamId = e.teamId)
        dispatch(invokeHandlerFunction(e, cm))

      case Left(failure) =>
        throw new RugRuntimeException(pathExpressionStr,
          s"Error evaluating path expression $pathExpression: [$failure]")
    }
  }

  override protected def invokeHandlerFunction(e: SystemEvent, cm: ContextMatch): Object = {
    handlerFunction.call(thiz, Event(cm))
  }

  /**
    * Extract all messages and use messageBuilder/actionRegistry to find and dispatch actions
    * @param plan
    */
  def dispatch(plan: Object): Unit = {
    plan match {
      case o: ScriptObjectMirror => {
        o.get("messages") match {
          case messages: ScriptObjectMirror if messages.isArray => {
            messages.values().asScala.foreach(msg => {
              val m = msg.asInstanceOf[ScriptObjectMirror]
              var responseMessage = ctx.messageBuilder.regarding(m.get("regarding").asInstanceOf[TreeNode])
              responseMessage =m.get("text") match {
                case text: String => responseMessage.say(text)
                case _ => responseMessage
              }
              responseMessage = m.get("channelId") match {
                case c: String => responseMessage.on(c)
                case _ => responseMessage
              }
              responseMessage = m.get("rugs") match {
                case rugs: ScriptObjectMirror if rugs.isArray => {
                  addActions(responseMessage, rugs)
                }
                case _ => responseMessage
              }
              responseMessage.send()
            })
          }
        }
      }
    }
  }

  /**
    * Extract action from JS and bind to Message
    *
    * Beware use of var!
    *
    * @param msg current message
    * @param rugs array of Rugs
    * @return new message
    */
  def addActions(msg: Message, rugs: ScriptObjectMirror) : Message = {
    var responseMessage = msg
    for (rug <- rugs.values().asScala) {
      val r = rug.asInstanceOf[ScriptObjectMirror]
      //TODO - this is a reimplementation of the @cd's label hack - but at least it's not in TS
      val actionName = r.get("label") match {
        case label: String => s"${r.get("name").asInstanceOf[String]}|$label"
        case _ => r.get("name").asInstanceOf[String]
      }
      var action = responseMessage.actionRegistry.findByName(actionName)
      r.get("params") match {
        case params: ScriptObjectMirror => {
          for (param <- params.entrySet().asScala) {
            action = responseMessage.actionRegistry.bindParameter(action, param.getKey, param.getValue)
          }
        }
        case _ =>
      }
      responseMessage = msg.withAction(action)
    }
    responseMessage
  }
}


/**
  * Represents an event that drives a handler
  *
  * @param cm the root node in the tree
  */
case class Event(cm: ContextMatch) {
  def child: TreeNode = cm.root.asInstanceOf[TreeNode]
}
