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
  * Uses SnakeYaml
  */
class YmlFileType extends TypeUnderFile {

  private sealed trait State {

    def handle(content: String, e: Event, nodeStack: mutable.Stack[ParsedMutableContainerTreeNode]): State
  }

  val yaml = new Yaml()

  override def description: String = "YAML file"

  override def isOfType(f: FileArtifact): Boolean =
    f.name.endsWith(".yml") | f.name.endsWith(".yaml")

  override def fileToRawNode(f: FileArtifact, ml: Option[MatchListener]): Option[PositionedTreeNode] = {

    var state: State = Open

    val root = new ParsedMutableContainerTreeNode(f.name)
    root.startPosition = OffsetInputPosition(0)
    root.endPosition = OffsetInputPosition(f.contentLength)

    val events: Iterable[Event] = yaml.parse(new InputStreamReader(f.inputStream())).asScala

    // Node we're currently adding to
    val nodeStack: mutable.Stack[ParsedMutableContainerTreeNode] = new mutable.Stack()
    nodeStack.push(root)

    for (e <- events) {
      println(e)
      state = state.handle(f.content, e, nodeStack)
    }

    validateStructure(root)
    root.pad(f.content)
    Some(root)
  }


  /**
    * We're open to a sequence of scala
    */
  private case object Open extends State {

    override def handle(content: String, e: Event, nodeStack: mutable.Stack[ParsedMutableContainerTreeNode]): State = e match {
      case s: ScalarEvent =>
        SeenKey(keyTerminal = scalarToTreeNode(content, s))
      //      case _: CollectionStartEvent | CollectionStartEvent =>
      //        Blank
      case _ =>
        // Ignore
        Open
    }
  }

  // TODO rewrite with partial functions to get inheritance, ignoring and returning same state in other cases
  private case class SeenKey(keyTerminal: PositionedTreeNode) extends State {

    override def handle(content: String, e: Event, nodeStack: mutable.Stack[ParsedMutableContainerTreeNode]): State =
      e match {
        case s: ScalarEvent =>
          // Scalar key with value. just get out of there
          val value = scalarToTreeNode(content, s)
          val container = SimpleMutableContainerTreeNode.wrap(keyTerminal.nodeName, Seq(value), significance = TreeNode.Signal)
          nodeStack.top.insertFieldCheckingPosition(container)
          Open
        case sse: SequenceStartEvent =>
          val newContainer = new ParsedMutableContainerTreeNode(keyTerminal.nodeName)
          newContainer.startPosition = markToPosition(content, sse, skipLeadingQuote = false)
          nodeStack.top.appendField(newContainer)
          nodeStack.push(newContainer)
          InCollection
      }
  }


  private case object InCollection extends State {

    override def handle(content: String, e: Event, nodeStack: mutable.Stack[ParsedMutableContainerTreeNode]): State = e match {
      case see: SequenceEndEvent =>
        nodeStack.top.endPosition = markToPosition(content, see, skipLeadingQuote = false)
        nodeStack.pop()
        Open
      case s: ScalarEvent =>
        val sf = scalarToTreeNode(content, s)
        //println(sf)
        nodeStack.top.insertFieldCheckingPosition(sf)
        InCollection
    }
  }


  private def scalarToTreeNode(in: String, se: ScalarEvent): PositionedTreeNode = {
    val name = if (se.getValue.exists(c => c.isWhitespace)) "scalar" else se.getValue
    val sf = new MutableTerminalTreeNode(name,
      se.getValue,
      markToPosition(in, se, skipLeadingQuote = true)
    )
    sf.addType("scalar")
    sf
  }

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

