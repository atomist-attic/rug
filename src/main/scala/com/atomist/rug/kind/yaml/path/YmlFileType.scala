package com.atomist.rug.kind.yaml.path

import java.io.InputStreamReader

import com.atomist.rug.kind.grammar.TypeUnderFile
import com.atomist.rug.kind.yaml.path.YamlFileType._
import com.atomist.source.FileArtifact
import com.atomist.tree.TreeNode
import com.atomist.tree.content.text._
import com.atomist.tree.content.text.grammar.MatchListener
import com.typesafe.scalalogging.LazyLogging
import org.apache.commons.io.IOUtils
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.error.Mark
import org.yaml.snakeyaml.events._

import scala.collection.JavaConverters._
import scala.collection.mutable

/**
  * Uses SnakeYaml.
  *
  * @deprecated Use [[YamlFileType]] instead
  */
@deprecated("Use YamlFileType instead", "0.13.0")
class YmlFileType extends TypeUnderFile with LazyLogging {

  private case class Input(content: String, event: Event, nodeStack: mutable.Stack[ParsedMutableContainerTreeNode])

  private abstract sealed class State {

    final def transition(in: Input): State = {
      val noTransitionOnIgnorableInput: PartialFunction[Input, State] = {
        case _ =>
          logger.debug(s"${this} ignored input: ${in.event}")
          this
      }
      on.orElse(noTransitionOnIgnorableInput)(in)
    }

    /**
      * Throw an exception on an illegal input. Otherwise handle only legal transitions,
      * as this class's transition method will return the current state without error
      */
    protected def on: PartialFunction[Input, State]
  }

  // SnakeYAML parser
  private val yaml = new Yaml()

  override def description: String = "YAML file"

  override def isOfType(f: FileArtifact): Boolean =
    f.name.endsWith(".yml") | f.name.endsWith(".yaml")

  /**
    * Uses a state machine to handle SnakeYAML events, which include node positions.
    */
  override def fileToRawNode(f: FileArtifact, ml: Option[MatchListener]): Option[PositionedTreeNode] = {
    val reader = new InputStreamReader(f.inputStream())
    val events: Iterable[Event] = yaml.parse(reader).asScala

    // We're currently adding to the node on top of the stack
    val nodeStack: mutable.Stack[ParsedMutableContainerTreeNode] = new mutable.Stack()
    var state: State = InDocument

    // TODO do this for each doc
    val root = new ParsedMutableContainerTreeNode(f.name)
    root.startPosition = OffsetInputPosition(0)
    root.endPosition = OffsetInputPosition(f.contentLength)
    nodeStack.push(root)

    for (e <- events) {
      val oldState = state
      state = state.transition(Input(f.content, e, nodeStack))
      // println(s"$e from $oldState to $state")
    }
    IOUtils.closeQuietly(reader)

    root.pad(f.content)
    Some(root)
  }

  private case object InDocument extends State {

    protected override def on = {
      case Input(content, s: ScalarEvent, _) =>
        SeenKey(keyTerminal = scalarToTreeNode(content, s), this)
    }
  }

  private case class SeenKey(keyTerminal: PositionedTreeNode, previousState: State)
    extends State {

    protected def on = {
      case Input(content, s: ScalarEvent, nodeStack) =>
        // Scalar key with value. Add a container a child of present node and return to Open state
        val value = scalarToTreeNode(content, s)
        val container = SimpleMutableContainerTreeNode.wrap(keyTerminal.value, Seq(value), significance = TreeNode.Signal)
        container.addType(ScalarType)
        nodeStack.top.insertFieldCheckingPosition(container)
        previousState
      case Input(content, sse: SequenceStartEvent, nodeStack) =>
        val containerName = if (canBeUsedAsNodeName(keyTerminal.value)) SequenceType else keyTerminal.value
        val newContainer = new ParsedMutableContainerTreeNode(containerName)
        newContainer.addType(SequenceType)
        newContainer.startPosition = markToPosition(content, sse.getStartMark)
        nodeStack.top.appendField(newContainer)
        nodeStack.push(newContainer)
        InSequence(previousState)
      case Input(content, mse: MappingStartEvent, nodeStack) =>
        val containerName = if (canBeUsedAsNodeName(keyTerminal.value)) MappingType else keyTerminal.value
        val newContainer = new ParsedMutableContainerTreeNode(containerName)
        newContainer.addType(MappingType)
        newContainer.startPosition = markToPosition(content, mse.getStartMark)
        nodeStack.top.appendField(newContainer)
        nodeStack.push(newContainer)
        InMapping(previousState)
    }
  }

  private case class InMapping(previousState: State) extends State {

    protected override def on = {
      case Input(content, see: MappingEndEvent, nodeStack) =>
        nodeStack.top.endPosition = markToPosition(content, see.getStartMark)
        nodeStack.pop()
        previousState
      case Input(content, s: ScalarEvent, _) =>
        SeenKey(keyTerminal = scalarToTreeNode(content, s), this)
    }
  }

  private case class InSequence(previousState: State) extends State {

    protected override def on = {
      case Input(content, see: SequenceEndEvent, nodeStack) =>
        nodeStack.top.endPosition = markToPosition(content, see.getStartMark)
        nodeStack.pop()
        previousState
      case Input(content, s: ScalarEvent, nodeStack) =>
        val sf = scalarToTreeNode(content, s)
        nodeStack.top.insertFieldCheckingPosition(sf)
        this
    }
  }

  /**
    * Value will be the full structure.
    */
  private def scalarToTreeNode(in: String, se: ScalarEvent): PositionedTreeNode = {
    val startPos = markToPosition(in, se.getStartMark)
    val fullValue = in.substring(
      startPos.offset,
      markToPosition(in, se.getEndMark).offset)
    val f = new MutableTerminalTreeNode(ScalarName, fullValue, startPos)
    f.addType(ScalarType)
    f
  }

  private def canBeUsedAsNodeName(s: String) = s.exists(_.isWhitespace)

  private def markToPosition(in: String, m: Mark): InputPosition = {
    // Snake YAML shows positions in toStrings from 1 but returns them from 0
    LineInputPositionImpl(in, m.getLine + 1, m.getColumn + 1)
  }
}





