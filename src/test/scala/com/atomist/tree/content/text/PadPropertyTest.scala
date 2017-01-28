package com.atomist.tree.content.text

import com.atomist.tree.{ContainerTreeNode, TerminalTreeNode, TreeNode}
import org.scalacheck.{Arbitrary, Gen, Prop, Shrink}
import org.scalatest.{FlatSpec, Matchers}
import org.scalatest.prop.Checkers
import org.scalacheck.Prop._

class PadPropertyTest extends FlatSpec with Checkers {

  implicit override val generatorDrivenConfig =
    PropertyCheckConfiguration(minSuccessful = 10)

  import PositionedTreeNodeGenerators._

  it should "Terminal tree nodes do not change their value when padded" in {
    check { (tn: PositionedTreeNode with TerminalTreeNode) =>
      // in the metamorphosis future, this would be
      // tn.value == pad(tn, _).value
      tn.padded
    }
  }

  it should "still have all terminal tree nodes after padding" in {
    check {
      Prop.forAll(ContainerNodeAndInputGen)({ case (node, inputText) =>
        val input = inputText.content
        val (padded, report) = AbstractMutableContainerTreeNode.pad(node, input, topLevel = true)
        val inputTerminals = collectTerminalNodes(node)
        val outputTerminals = collectTerminalNodes(padded)
        // no input terminals are missing from the output terminals
        all(
          inputTerminals.map {
            in =>
              Prop(outputTerminals.exists { out => out.value == in.value }).
                label(s"No output terminal node exists for input ${in}. Outputs are: ${outputTerminals.mkString("\n")}").
                label(report.mkString("---", "\n", "---") + "FARRT")
          }: _*)
      })
    }
  }


  private def collectTerminalNodes(node: TreeNode, others: Seq[TerminalTreeNode] = Seq()): Seq[TerminalTreeNode] =
    node match {
      case tn: TerminalTreeNode => Seq(tn) ++ others
      case cn: ContainerTreeNode => cn.childNodes.flatMap(collectTerminalNodes(_)) ++ others
    }
}

object PositionedTreeNodeGenerators {

  val MaxReasonableInputLen = 10

  val ZeroOffset = OffsetInputPosition(0)
  val MaxOffset = OffsetInputPosition(MaxReasonableInputLen)

  def atMost(chars: Int)(str: String) = str.substring(0, Math.min(str.length, chars))

  val NodeNameGen = Gen.alphaStr.map(atMost(10))

  def offsetBetween(start: OffsetInputPosition, end: OffsetInputPosition): Gen[OffsetInputPosition] =
    Gen.choose(start.offset, end.offset).map(OffsetInputPosition(_))

  val ReasonableOffset: Gen[OffsetInputPosition] = offsetBetween(ZeroOffset, MaxOffset)

  // TODO: use more varied characters
  private def stringOfLength(n: Int) = Gen.listOfN(n, Gen.alphaNumChar).map(_.mkString)

  def terminalTreeNodeSpanning(start: OffsetInputPosition, end: OffsetInputPosition) =
    for {
      name <- NodeNameGen
      initialValue <- stringOfLength(end.offset - start.offset)
    } yield new MutableTerminalTreeNode(name, initialValue, start)

  def containerTreeNodeSpanning(start: OffsetInputPosition, end: OffsetInputPosition) =
    for {
      name <- NodeNameGen
      startOffset <- offsetBetween(start, end)
      endOffset <- offsetBetween(startOffset, end)
      howMany <- Gen.choose(0, Math.max(3, endOffset - startOffset + 1)) // can we have nodes in empty containers? seems likely.
      children <- Gen.listOfN(howMany, restrainedPositionedTreeNodeGen(startOffset, endOffset)) // TODO: restrain offsets

    } yield new SimpleMutableContainerTreeNode(name, children, startOffset, endOffset)

  def restrainedPositionedTreeNodeGen(start: OffsetInputPosition, end: OffsetInputPosition): Gen[PositionedTreeNode] =
    for {
      startOffset <- offsetBetween(start, end)
      endOffset <- offsetBetween(startOffset, end)
      tn <- positionedTreeNodeSpanning(startOffset, endOffset)
    } yield tn

   def restrainedPositionedContainerTreeNodeGen(start: OffsetInputPosition, end: OffsetInputPosition): Gen[PositionedTreeNode] =
    for {
      startOffset <- offsetBetween(start, end)
      endOffset <- offsetBetween(startOffset, end)
      tn <- containerTreeNodeSpanning(startOffset, endOffset)
    } yield tn

  def positionedTreeNodeSpanning(start: OffsetInputPosition, end: OffsetInputPosition): Gen[PositionedTreeNode] =
    Gen.oneOf(terminalTreeNodeSpanning(start, end), containerTreeNodeSpanning(start, end))

  val PositionedTreeNodeGen: Gen[PositionedTreeNode] =
    for {
      startOffset <- ReasonableOffset
      endOffset <- ReasonableOffset if startOffset.offset <= endOffset.offset
      tn <- restrainedPositionedTreeNodeGen(startOffset, endOffset)
    }
      yield tn

  val PositionedContainerTreeNodeGen: Gen[PositionedTreeNode] =
    for {
      startOffset <- ReasonableOffset
      endOffset <- ReasonableOffset if startOffset.offset <= endOffset.offset
      tn <- restrainedPositionedContainerTreeNodeGen(startOffset, endOffset)
    }
      yield tn

  private[text] def overwriteSegment(in: String, start: Int, out: String): String =
    in.substring(0, start) + out + in.substring(start + out.length, in.length)

  def cromulentInputFor(node: PositionedTreeNode): Gen[InputText] = {
    val minimumLength = node.endPosition.offset
    for {len <- Gen.choose(minimumLength, MaxReasonableInputLen)
         input <- stringOfLength(len)} yield {

      def overwrite(input: String, node: PositionedTreeNode): String =
        node match {
          case tn: TerminalTreeNode =>
            val p = overwriteSegment(input, tn.startPosition.offset, tn.value)
            p
          case cn: ContainerTreeNode =>
            cn.childNodes.map(_.asInstanceOf[PositionedTreeNode]).foldLeft(input)(overwrite)
        }

      val q = overwrite(input, node)
      InputText(q)
    }
  }

  val ContainerNodeAndInputGen: Gen[(PositionedTreeNode, InputText)] =
    for {
      node <- PositionedContainerTreeNodeGen
      input <- cromulentInputFor(node)
    } yield {
      (node, input)
    }

  case class InputText(content: String)

  // don't shrink this yo

  implicit val ShrinkPositionedTreeNode: Shrink[PositionedTreeNode] = Shrink { node: PositionedTreeNode =>
    node match {
      case tn: TerminalTreeNode =>
        if (tn.value.isEmpty) Stream.empty else {
          val oneFewerChar = new MutableTerminalTreeNode(tn.nodeName, tn.value.substring(0, tn.value.length - 1), tn.startPosition)
          Stream(oneFewerChar) /*append Shrink.shrink(oneFewerChar)*/
        }
      case ctn: ContainerTreeNode =>
        (for {
          goodbyeChildIndex <- Range(0, ctn.childNodes.length)
        } yield {
          val before = ctn.childNodes.slice(0, goodbyeChildIndex)
          val goodbyeChild = ctn.childNodes(goodbyeChildIndex)
          val after = ctn.childNodes.slice(goodbyeChildIndex, ctn.childNodes.length)
          // take this one out
          val listWithout = before ++ after
          val without: Stream[PositionedTreeNode] = Stream(new SimpleMutableContainerTreeNode(ctn.nodeName, listWithout, ctn.startPosition, ctn.endPosition))
          val withShrunkChild: Stream[PositionedTreeNode] = Shrink.shrink(goodbyeChild).map { alteredChild =>
            new SimpleMutableContainerTreeNode(ctn.nodeName, before ++ Seq(alteredChild) ++ after, ctn.startPosition, ctn.endPosition)
          }
          without.append(withShrunkChild)
        }).reduce(_ append _)

    }
  }

  implicit val ArbitraryNodeAndInput: Arbitrary[(PositionedTreeNode, InputText)] = Arbitrary(ContainerNodeAndInputGen)
  implicit val ArbitraryPositionedTreeNode: Arbitrary[PositionedTreeNode] = Arbitrary(PositionedTreeNodeGen)
  implicit val ArbitraryTerminalTreeNode: Arbitrary[PositionedTreeNode with TerminalTreeNode] = Arbitrary(terminalTreeNodeSpanning(ZeroOffset, MaxOffset))
}

class TestTheTests extends FlatSpec with Matchers {

  it should "Overwrite a piece of string" in {
    PositionedTreeNodeGenerators.overwriteSegment("crunchy banana pancakes", 3, "foo") should be("crufooy banana pancakes")
  }

}