package com.atomist.tree.content.text.grammar.antlr

import com.atomist.tree.{ContainerTreeNode, SimpleTerminalTreeNode, TreeNode}
import com.atomist.tree.content.text.grammar.{MatchListener, PositionalString}
import com.atomist.tree.content.text._
import com.typesafe.scalalogging.LazyLogging
import org.antlr.v4.runtime.tree.{ErrorNode, TerminalNode}
import org.antlr.v4.runtime.{ParserRuleContext, Token}
import org.snt.inmemantlr.DefaultListener

import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer

object Excludes {

  /** Antlr generated-class method names to exclude */
  val ExcludedMethods = Set("getRuleIndex")
}

/**
  * Builds our TreeNode model from Antlr callbacks.
  *
  * @param matchRule
  */
class ModelBuildingListener(
                             matchRule: String,
                             ml: Option[MatchListener])
  extends DefaultListener with LazyLogging {

  private val _results = new ListBuffer[MutableContainerTreeNode]()

  def results: Seq[MutableContainerTreeNode] = _results

  override def enterEveryRule(ctx: ParserRuleContext): Unit = {
    val rule = this.getRuleByKey(ctx.getRuleIndex)
    rule match {
      case `matchRule` =>
        logger.debug(s"enterEveryRule $rule: ${ctx.getClass} ${ctx.getText}")
        if (ctx.exception != null) {
          // Failed to get a whole match
          // ctx.exception.printStackTrace()
        } else {
          val mof = treeToContainerField(ctx)
          _results append mof
          ml.foreach(_.onMatch(mof))
          logger.debug("\t" + ctx)
        }
      case _ =>
    }
  }

  // Use reflection to extract information from the generated methods and fields in this class
  private def treeToContainerField(rc: ParserRuleContext): AbstractMutableContainerTreeNode = {
    val rule = this.getRuleByKey(rc.getRuleIndex)

    if (rc.exception != null) throw rc.exception

    val startPos = // position(rc.getStart)
    OffsetInputPosition(rc.getStart.getStartIndex)
    val endPos = // position(rc.stop) + rc.getStop.getText.size
    OffsetInputPosition(rc.getStop.getStopIndex + 1)

    // Create an empty model node that we'll fill
    val mof = new SimpleMutableContainerTreeNode(rule, Nil, startPos, endPos)

    // We only want methods on the generated class itself
    val valueMethods = rc.getClass
      .getDeclaredMethods
      .filter(m => !Excludes.ExcludedMethods.contains(m.getName))

    val fieldsToAdd = ListBuffer.empty[TreeNode]

    def addField(f: TreeNode): Unit = {
      fieldsToAdd.append(f)
    }

    for {
      m <- valueMethods
      if m.getParameterCount == 0
    } {
      val value = m.invoke(rc)
      for (f <- makeField(m.getName, value, m.getReturnType))
        addField(f)
    }

    for (f <- rc.getClass.getDeclaredFields) {
      val value = f.get(rc)
      for (mf <- makeField(f.getName, value, f.getType))
        addField(mf)
    }

    val deduped = deduplicate(fieldsToAdd)
    for {
      f <- deduped
    }
      mof.insertFieldCheckingPosition(f)
    mof
  }

  // Remove duplicate fields. The ones with lower case can replace the ones with upper case
  // For example, handle case method_name=IDENTIFIER
  private def deduplicate(fields: Seq[TreeNode]): Seq[TreeNode] = {
    val lexerPred: TreeNode => Boolean = f => f.nodeName(0).isUpper
    val lexerFields = fields.filter(lexerPred)
    val positionedParserFields = fields.filter(!lexerPred(_)) collect {
      case pv: PositionedTreeNode => pv
    }

    def theresAnAliasFieldWithProvablySamePosition(what: TreeNode) = what match {
      case pf: PositionedTreeNode =>
        positionedParserFields.exists(ppf => ppf.startPosition.offset == pf.startPosition.offset)
      case _ => false
    }

    fields.filter(f => !(lexerFields.contains(f) && theresAnAliasFieldWithProvablySamePosition(f)))
  }

  private def position(tok: Token): InputPosition = {
    val input = tok.getInputStream.toString
    // Antlr indexes lines from 0 and columns from 1.
    val pos = tok.getStartIndex match {
      case -1 => LineInputPositionImpl(input, tok.getLine, tok.getCharPositionInLine + 1)
      case n => OffsetInputPosition(n)
    }
    pos
  }

  private def makeField(name: String, value: Object, typ: Class[_]): Seq[TreeNode] = {
    val r = value match {
      case en: ErrorNode =>
        logger.info(s"ErrorNode: $name=$en")
        val sf = SimpleTerminalTreeNode(name, "")
        Seq(sf)
      case ct: Token if ct.getText.startsWith(s"<missing ") =>
        // Handle Antlr empty values
        val sf = new MutableTerminalTreeNode(name, "", position(ct))
        Seq(sf)
      case ct: Token =>
        val sf = new MutableTerminalTreeNode(name, ct.getText, position(ct))
        Seq(sf)
      case tn: TerminalNode =>
        makeField(name, tn.getSymbol, classOf[String])
      case l: java.util.List[Object @unchecked] =>
        l.asScala.flatMap(e => makeField(name, e, classOf[Object]))
      case prc: ParserRuleContext =>
        Seq(treeToContainerField(prc))
      case null =>
        // It's valid to reference this field, but it should produce nothing. So return an empty collection.
        // However, populate it with the possible field names
        val possibleFieldNames =
          typ.getDeclaredMethods.map(_.getName) ++ typ.getDeclaredFields.map(_.getName)
        Seq(EmptyContainerTreeNode(name, possibleFieldNames.toSet))
    }
    r
  }
}

/**
  * Empty container field value including fieldName information about possible fields,
  * that are not present in this instance. This allows Rug type checking to work.
  */
case class EmptyContainerTreeNode(nodeName: String, override val childNodeNames: Set[String])
  extends ContainerTreeNode {

  override def childNodes: Seq[TreeNode] = Nil

  override def nodeType: String = "empty"

  override def childNodeTypes: Set[String] = Set()

  override def value: String = ""
}
