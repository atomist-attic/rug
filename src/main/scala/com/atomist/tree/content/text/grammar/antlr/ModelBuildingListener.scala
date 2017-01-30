package com.atomist.tree.content.text.grammar.antlr

import com.atomist.tree.content.text._
import com.atomist.tree.content.text.grammar.MatchListener
import com.atomist.tree.{ContainerTreeNode, SimpleTerminalTreeNode, TreeNode}
import com.typesafe.scalalogging.LazyLogging
import org.antlr.v4.runtime.tree.{ErrorNode, TerminalNode}
import org.antlr.v4.runtime.{ParserRuleContext, Token}
import org.snt.inmemantlr.listener.DefaultListener

import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer

object Excludes {

  /** Antlr generated-class method names to exclude */
  val ExcludedMethods = Set("getRuleIndex")
}

/**
  * Builds our TreeNode model from Antlr callbacks.
  *
  * @param matchRule name of the rule (production) we want to match
  */
class ModelBuildingListener(
                             matchRule: String,
                             ml: Option[MatchListener])
  extends DefaultListener with LazyLogging {

  private val results = new ListBuffer[MutableContainerTreeNode]()

  /**
    * @return the nodes corresponding to these rules
    */
  def ruleNodes: Seq[MutableContainerTreeNode] = results

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
          ml.foreach(_.onMatch(mctn))
          logger.debug("\t" + ctx)
        }
      case _ =>
      // Ignore this production
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


    // We only want methods on the generated class itself
    val valueMethods = rc.getClass
      .getDeclaredMethods
      .filter(m => !Excludes.ExcludedMethods.contains(m.getName))

    val fieldsToAdd = ListBuffer.empty[TreeNode]

    def addField(f: TreeNode): Unit = f match {
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
      for (f <- makeField(m.getName, value, m.getReturnType))
        addField(f)
    }

    for (f <- rc.getClass.getDeclaredFields) {
      val value = f.get(rc)
      for (mf <- makeField(f.getName, value, f.getType))
        addField(mf)
    }

    val deduped = deduplicate(fieldsToAdd)
    new SimpleMutableContainerTreeNode(rule, deduped, startPos, endPos, Set(rule))
  }

  // Remove duplicate fields. The ones with lower case can replace the ones with upper case
  // For example, handle case method_name=IDENTIFIER
  private def deduplicate(fields: Seq[TreeNode]): Seq[TreeNode] = {
    val lexerPred: TreeNode => Boolean = f => f.nodeName(0).isUpper
    val lexerFields = fields.filter(lexerPred)

    val positionedParserFields = fields
      .filter(!lexerPred(_)) collect {
      case pv: PositionedTreeNode => pv
    }

    // There should be no overlap between these collections
    require(!lexerFields.exists(positionedParserFields.contains(_)))

    def theresAnAliasFieldWithProvablySamePosition(what: TreeNode) = what match {
      case pf: PositionedTreeNode =>
        positionedParserFields.exists(ppf => pf.hasSamePositionAs(ppf))
      case _ => false
    }

    // We don't want lexer fields that have an alias field with the same position
    val deduped = fields.filter(f => !(lexerFields.contains(f) &&
      theresAnAliasFieldWithProvablySamePosition(f))
    )

    def unwantedDuplicates(fields: Seq[TreeNode]): Seq[TreeNode] =
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

  private def makeField(name: String, value: Object, typ: Class[_]): Seq[TreeNode] = {
    val r = value match {
      case en: ErrorNode =>
        logger.info(s"ErrorNode: $name=$en")
        val sf = SimpleTerminalTreeNode(name, "", Set(name))
        Seq(sf)
      case ct: Token if ct.getText.startsWith(s"<missing ") =>
        // Handle Antlr empty values
        val sf = new MutableTerminalTreeNode(name, "", position(ct))
        sf.addType(name)
        Seq(sf)
      case ct: Token if ct.getText == "<EOF>" =>
        // For some reason, some grammars put the EOF on the token input stream
        Nil
      case ct: Token =>
        val sf = new MutableTerminalTreeNode(name, ct.getText, position(ct))
        sf.addType(name)
        Seq(sf)
      case tn: TerminalNode =>
        makeField(name, tn.getSymbol, classOf[String])
      case l: java.util.List[Object@unchecked] =>
        l.asScala.flatMap(e => makeField(name, e, classOf[Object]))
      case prc: ParserRuleContext =>
        Seq(treeToContainerField(prc))
      case null =>
        // It's valid to reference this field, but it should produce nothing. So return an empty collection.
        // However, populate it with the possible field names
        val possibleFieldNames =
          typ.getDeclaredMethods.map(_.getName) ++ typ.getDeclaredFields.map(_.getName)
        Seq(EmptyAntlrContainerTreeNode(name, possibleFieldNames.toSet, Set(name)))
    }
    r
  }
}

/**
  * Empty container field value including fieldName information about possible fields,
  * that are not present in this instance. This allows Rug type checking to work.
  */
case class EmptyAntlrContainerTreeNode(nodeName: String,
                                       override val childNodeNames: Set[String],
                                       types: Set[String] = Set())
  extends ContainerTreeNode {

  override def childNodes: Seq[TreeNode] = Nil

  override def childrenNamed(key: String): Seq[TreeNode] = Nil

  override def nodeType: Set[String] = Set("empty") ++ types

  override def childNodeTypes: Set[String] = Set()

  override def value: String = ""
}
