package com.atomist.tree.content.text.grammar.antlr

import com.atomist.tree.TreeNode
import com.atomist.tree.TreeNode.Significance
import com.atomist.tree.content.text._
import com.typesafe.scalalogging.LazyLogging
import org.antlr.v4.runtime.tree.{ErrorNode, TerminalNode}
import org.antlr.v4.runtime.{ParserRuleContext, Token}
import org.snt.inmemantlr.listener.DefaultListener

import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer

object Excludes {

  /** Antlr generated-class method names to exclude. */
  val ExcludedMethods = Set("getRuleIndex")
}

/**
  * Builds our TreeNode model from Antlr callbacks.
  *
  * @param matchRule name of the rule (production) we want to match
  */
class ModelBuildingListener(
                             matchRule: String,
                             namingStrategy: AstNodeCreationStrategy)
  extends DefaultListener with LazyLogging {

  private val results = new ListBuffer[PositionedTreeNode]()

  /**
    * @return the nodes corresponding to these rules
    */
  def ruleNodes: Seq[PositionedTreeNode] = results

  override def enterEveryRule(ctx: ParserRuleContext): Unit = {
    val rule = this.getRuleByKey(ctx.getRuleIndex)
    rule match {
      case `matchRule` =>
        logger.debug(s"enterEveryRule $rule: ${ctx.getClass} ${ctx.getText}")
        if (ctx.exception != null) {
          // Failed to get a whole match
          // ctx.exception.printStackTrace()
        }
        else {
          val mctn = treeToContainerField(ctx)
          results.append(mctn)
          logger.debug("\t" + ctx)
        }
      case _ =>
      // Ignore this production
    }
  }

  // Use reflection to extract information from the generated methods and fields in this class
  private def treeToContainerField(rc: ParserRuleContext): AntlrPositionedTreeNode = {
    val rule = this.getRuleByKey(rc.getRuleIndex)

    if (rc.exception != null) throw rc.exception

    val startPos = // position(rc.getStart)
      OffsetInputPosition(rc.getStart.getStartIndex)
    val endPos = // position(rc.stop) + rc.getStop.getText.size
      OffsetInputPosition(rc.getStop.getStopIndex + 1)

    // We only want methods on the generated class itself
    val valueMethods = rc.getClass
      .getDeclaredMethods
      .filter(m => !Excludes.ExcludedMethods.contains(m.getName))

    val fieldsToAdd = ListBuffer.empty[AntlrPositionedTreeNode]

    def addField(f: AntlrPositionedTreeNode): Unit = f match {
      case ptn: PositionedTreeNode =>
        if (!fieldsToAdd.exists {
          case p: PositionedTreeNode if ptn.hasSamePositionAs(p) => true
          case _ => false
        })
          fieldsToAdd.append(f)
      case tn =>
        fieldsToAdd.append(tn)
    }

    for {
      m <- valueMethods
      if m.getParameterCount == 0
    } {
      val value = m.invoke(rc)
      for (f <- makeField(m.getName, value))
        addField(f)
    }

    for (f <- rc.getClass.getDeclaredFields) {
      val value = f.get(rc)
      for (mf <- makeField(f.getName, value))
        addField(mf)
    }

    val deduped = deduplicate(fieldsToAdd)
    val namey = namingStrategy.nameForContainer(rule, deduped)
    AntlrPositionedTreeNode.parent(
      namey,
      startPos,
      endPos,
      deduped,
      namingStrategy.tagsForContainer(rule, deduped) ++ Set(TreeNode.Dynamic),
      namingStrategy.significance(rule, deduped))
  }

  // Remove duplicate fields. The ones with lower case can replace the ones with upper case
  // For example, handle case method_name=IDENTIFIER
  private def deduplicate(fields: Seq[AntlrPositionedTreeNode]): Seq[AntlrPositionedTreeNode] = {
    val lexerPred: TreeNode => Boolean = f => f.nodeName.charAt(0).isUpper
    val lexerFields = fields.filter(lexerPred)

    val positionedParserFields = fields
      .filter(!lexerPred(_)) collect {
      case pv: PositionedTreeNode => pv
    }

    // There should be no overlap between these collections
    require(!lexerFields.exists(positionedParserFields.contains(_)))

    def theresAnAliasFieldWithProbablySamePosition(what: TreeNode) = what match {
      case pf: PositionedTreeNode =>
        positionedParserFields.exists(ppf => pf.hasSamePositionAs(ppf))
      case _ => false
    }

    // We don't want lexer fields that have an alias field with the same position
    val deduped = fields.filter(f => !(lexerFields.contains(f) &&
      theresAnAliasFieldWithProbablySamePosition(f))
    )

    def unwantedDuplicates(fields: Seq[AntlrPositionedTreeNode]): Seq[AntlrPositionedTreeNode] =
      if (fields.size <= 1) Nil
      else fields.head match {
        case pm: PositionedTreeNode =>
          fields.tail.collect {
            case pm2: PositionedTreeNode if pm.hasSamePositionAs(pm2) =>
              pm2
          }
        case _ => unwantedDuplicates(fields.tail)
      }

    val unwanted = unwantedDuplicates(deduped)
    val filtered = deduped.filterNot(unwanted.contains(_))
    require(unwantedDuplicates(filtered).isEmpty)
    filtered
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

  private def makeField(name: String, value: Object): Seq[AntlrPositionedTreeNode] = {
    val r = value match {
      case en: ErrorNode =>
        logger.info(s"ErrorNode: $name=$en")
        Seq() // does this have position information?
      case ct: Token if ct.getText.startsWith(s"<missing ") =>
        // Handle Antlr empty values
        val sf = AntlrPositionedTreeNode.leaf(name, position(ct), "", Set(name), TreeNode.Signal)
        Seq(sf)
      case ct: Token if ct.getText == "<EOF>" =>
        // For some reason, some grammars put the EOF on the token input stream
        Seq()
      case ct: Token =>
        val pos = position(ct)
        val text = ct.getText
        val namey = namingStrategy.nameForTerminal(name, text)
        val sf = AntlrPositionedTreeNode.leaf(
          namey,
          pos,
          text,
          Set(name), TreeNode.Signal)
        Seq(sf)
      case tn: TerminalNode =>
        makeField(name, tn.getSymbol)
      case l: java.util.List[Object@unchecked] =>
        l.asScala.flatMap(e => makeField(name, e))
      case prc: ParserRuleContext =>
        Seq(treeToContainerField(prc))
      case null =>
        Seq()
    }
    r
  }
}

/**
  * Enables us to rename, tag and determine significance of nodes from an Antlr grammar.
  * Contains default implementation. Subclasses can simply override the
  * methods they care about
  */
trait AstNodeCreationStrategy {

  def nameForTerminal(rawName: String, content: String): String = rawName

  def nameForContainer(rule: String, fields: Seq[AntlrPositionedTreeNode]): String = rule

  def tagsForContainer(rule: String, fields: Seq[AntlrPositionedTreeNode]): Set[String] = Set(rule)

  def significance(rule: String, fields: Seq[AntlrPositionedTreeNode]): TreeNode.Significance =
    TreeNode.Signal
}

/**
  * Default implementation of AstNodeNamingStrategy that takes node names from
  * underlying Antlr grammar.
  */
object FromGrammarAstNodeCreationStrategy extends AstNodeCreationStrategy

/*
 * This has position, name, tags, value, significance
 * PositionedTreeNode doesn't really need value, but it's helpful in NodeCreationStrategy methods.
 */
case class AntlrPositionedTreeNode(override val nodeName: String,
                                   override val startPosition: InputPosition,
                                   override val endPosition: InputPosition,
                                   override val childNodes: Seq[AntlrPositionedTreeNode],
                                   override val nodeTags: Set[String],
                                   override val significance: Significance,
                                   valueOption: Option[String])
  extends PositionedTreeNode {

  override def value: String = valueOption.getOrElse("")

  override def childNodeNames: Set[String] = ???

  override def childNodeTypes: Set[String] = ???

  override def childrenNamed(key: String): Seq[TreeNode] = childNodes.filter(_.nodeName == key)
}

object AntlrPositionedTreeNode {

  def leaf(nodeName: String,
           startPosition: InputPosition,
           value: String,
           nodeTags: Set[String],
           significance: Significance): AntlrPositionedTreeNode =
    AntlrPositionedTreeNode(
      nodeName,
      startPosition,
      startPosition + value.length,
      Seq(),
      nodeTags,
      significance,
      Some(value)
    )

  def parent(nodeName: String,
             startPosition: InputPosition,
             endPosition: InputPosition,
             childNodes: Seq[AntlrPositionedTreeNode],
             nodeTags: Set[String],
             significance: Significance): AntlrPositionedTreeNode =
    AntlrPositionedTreeNode(
      nodeName,
      startPosition,
      endPosition,
      childNodes,
      nodeTags,
      significance,
      None
    )

}
