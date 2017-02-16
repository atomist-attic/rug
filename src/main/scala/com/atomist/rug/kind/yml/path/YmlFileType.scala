package com.atomist.rug.kind.yml.path

import java.io.InputStreamReader

import com.atomist.rug.kind.grammar.TypeUnderFile
import com.atomist.source.FileArtifact
import com.atomist.tree.TreeNode
import com.atomist.tree.content.text._
import com.atomist.tree.content.text.grammar.MatchListener
import com.atomist.util.{Visitable, Visitor}
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.events._

import scala.collection.JavaConverters._
import scala.collection.mutable

/**
  * Uses SnakeYaml.
  */
class YmlFileType extends TypeUnderFile {

  import YmlFileType._

  private case class Input(content: String, event: Event, nodeStack: mutable.Stack[ParsedMutableContainerTreeNode])

  private abstract sealed class State {

    final def transition(transitionPayload: Input): State = {
      val identity: PartialFunction[Input, State] = {
        case _ => this
      }
      on.orElse(identity)(transitionPayload)
    }

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
    var state: State = SeekingKey

    val root = new ParsedMutableContainerTreeNode(f.name)
    root.startPosition = OffsetInputPosition(0)
    root.endPosition = OffsetInputPosition(f.contentLength)

    val events: Iterable[Event] = yaml.parse(new InputStreamReader(f.inputStream())).asScala

    // Node we're currently adding to
    val nodeStack: mutable.Stack[ParsedMutableContainerTreeNode] = new mutable.Stack()
    nodeStack.push(root)

    for (e <- events) {
      state = state.transition(Input(f.content, e, nodeStack))
    }

    validateStructure(root)
    root.pad(f.content)
    Some(root)
  }

  
  private case object SeekingKey extends State {

    protected override def on = {
      case Input(content, s: ScalarEvent, _) =>
        SeenKey(keyTerminal = scalarToTreeNode(content, s))
    }
  }

  private case class SeenKey(keyTerminal: PositionedTreeNode) extends State {

    protected def on = {
        case Input(content, s: ScalarEvent, nodeStack) =>
          // Scalar key with value. Add as a child of present node and return to Open state
          val value = scalarToTreeNode(content, s)
          val container = SimpleMutableContainerTreeNode.wrap(keyTerminal.nodeName, Seq(value), significance = TreeNode.Signal)
          nodeStack.top.insertFieldCheckingPosition(container)
          SeekingKey
        case Input(content, sse: SequenceStartEvent, nodeStack) =>
          val containerName = if (canBeUsedAsNodeName(keyTerminal.value)) SequenceType else keyTerminal.value
          val newContainer = new ParsedMutableContainerTreeNode(containerName)
          newContainer.addType(SequenceType)
          newContainer.startPosition = markToPosition(content, sse, skipLeadingQuote = false)
          nodeStack.top.appendField(newContainer)
          nodeStack.push(newContainer)
          InCollection
      }
  }

  private case object InCollection extends State {

    protected override def on = {
      case Input(content, see: SequenceEndEvent, nodeStack) =>
        nodeStack.top.endPosition = markToPosition(content, see, skipLeadingQuote = false)
        nodeStack.pop()
        SeekingKey
      case Input(content, s: ScalarEvent, nodeStack) =>
        val sf = scalarToTreeNode(content, s)
        nodeStack.top.insertFieldCheckingPosition(sf)
        InCollection
    }
  }


  private def scalarToTreeNode(in: String, se: ScalarEvent): PositionedTreeNode = {
    val name = if (canBeUsedAsNodeName(se.getValue)) ScalarType else se.getValue
    val sf = new MutableTerminalTreeNode(name,
      se.getValue,
      markToPosition(in, se, skipLeadingQuote = true)
    )
    sf.addType(ScalarType)
    sf
  }

  private def canBeUsedAsNodeName(s: String) = s.exists(c => c.isWhitespace)

  /**
    * Strip the leading " if necessary
    */
  private def markToPosition(in: String, s: Event, skipLeadingQuote: Boolean): InputPosition = {
    val m = s.getStartMark
    // Snake YAML shows positions in toStrings from 1 but returns them from 0
    val rawOff = LineInputPositionImpl(in, m.getLine + 1, m.getColumn + 1)
    // Special handling if we have a " included in the content. We skip it
    val off = if (skipLeadingQuote && in.charAt(rawOff.offset) == '"')
      rawOff + 1
    else
      rawOff
    s match {
      case se: ScalarEvent =>
        val calculatedValue = in.substring(off.offset, off.offset + se.getValue.length)
        require(calculatedValue == se.getValue,
          s"calculated [$calculatedValue] but expected [${se.getValue}]")
      case _ =>
    }
    off
  }

  private def validateStructure(tn: TreeNode): Unit = {
    val v = new Visitor {
      override def visit(v: Visitable, depth: Int): Boolean = {
        v match {
          case ptn: PositionedTreeNode =>
            require(ptn.startPosition != null, s"Node with name [${ptn.nodeName}] must have start position set")
            require(ptn.endPosition != null, s"Node with name [${ptn.nodeName}] must have end position set")
        }
        true
      }
    }
    tn.accept(v, 0)
  }

}


object YmlFileType {

  val SequenceType = "Sequence"

  val KeyType = "Key"

  val ScalarType = "Scalar"
}