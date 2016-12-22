package com.atomist.tree.pathexpression

import java.util.Objects

import com.atomist.source.StringFileArtifact
import com.atomist.tree.pathexpression.PathExpressionEngine._
import com.atomist.tree.content.text.TreeNodeOperations._
import com.atomist.tree.{ContainerTreeNode, TreeNode}
import com.atomist.util.scalaparsing.CommonTypesParser

/**
  * Scala parser combinator for path expressions
  */
trait PathExpressionParser extends CommonTypesParser {

  private def nodeName: Parser[String] = identifierRefString(Set(), PathProperty)

  private def objectType: Parser[String] = identifierRefString(Set(), ident)

  private def child: Parser[AxisSpecifier] = opt("child::") ^^
    (s => Child)

  private def nodeTypeTest: Parser[ObjectType] = objectType <~ "()" ^^ (p => ObjectType(p))

  private def test(extracted: Any, value: Any, n: TreeNode): Boolean = {
    value.equals(extracted)
  }

  private def propertyTest: Parser[Predicate] = "@" ~> nodeName ~ EqualsToken ~ singleQuotedString ^^ {
    case prop ~ op ~ literal =>
      val f: TreeNode => Boolean = prop match {
        case "name" =>
          n => test(n.nodeName, literal, n)
        case "type" =>
          n => test(n.nodeType, literal, n)
        case propName =>
          n => {
            n match {
              case ctn: ContainerTreeNode =>
                val extracted = ctn(prop)
                if (extracted.size == 1)
                  test(extracted.head, literal, n)
                else
                  false
              case _ => false
            }
          }
        case _ => throw new IllegalArgumentException(s"Cannot access property [$prop]")
      }
      SimplePredicate(s"$prop=$literal", (tn, _) => f(tn))
  }

  private def nullLiteral: Parser[Object] = "null" ^^ (_ => null)

  private def integer: Parser[Integer] = decimalNumber ^^ (s => s.toInt)

  private def literal: Parser[Any] = nullLiteral | singleQuotedString | integer

  private def methodInvocationTest: Parser[Predicate] = "." ~> nodeName ~ args ~ EqualsToken ~ literal ^^ {
    case methodName ~ args ~ op ~ literal =>
      SimplePredicate(s".$methodName", (n, among) => {
        val invoked = invokeMethod[Any](n, methodName, args)
        Objects.equals(literal, invoked)
      })
  }

  private def arg: Parser[String] = singleQuotedString

  private def args: Parser[Seq[String]] = "(" ~> repsep(arg, ",") <~ ")"

  private def booleanMethodInvocation: Parser[Predicate] = "." ~> nodeName ~ args ^^ {
    case methodName ~ args =>
      SimplePredicate(s".$methodName", (n, among) => invokeMethod[Boolean](n, methodName, args))
  }

  private def index: Parser[Predicate] = integer ^^ {
    case n => new IndexPredicate(s"[$n]", n)
  }

  private def truePredicate: Parser[Predicate] = "true" ^^ (_ => TruePredicate)

  private def falsePredicate: Parser[Predicate] = "false" ^^ (_ => FalsePredicate)

  private def predicateTerm: Parser[Predicate] = methodInvocationTest | propertyTest | booleanMethodInvocation |
    truePredicate | falsePredicate |
    index

  private def negatedPredicate: Parser[Predicate] = "not" ~> "(" ~> predicateExpression <~ ")" ^^ {
    case pred => new NegationOf(pred)
  }

  private def logicalOp: Parser[String] = "and" | "or"

  private def predicateAnd: Parser[Predicate] = predicateTerm ~ logicalOp ~ predicateExpression ^^ {
    case a ~ "and" ~ b => a and b
    case a ~ "or" ~ b => a or b
  }

  private def predicateExpression: Parser[Predicate] = predicateAnd | negatedPredicate | predicateTerm

  private def predicate: Parser[Predicate] = PredicateOpen ~> predicateExpression <~ PredicateClose

  private def nodeNameTest: Parser[NodeTest] = nodeName ^^
    (s => NamedNodeTest(s))

  private def allNodes: Parser[NodeTest] = "*" ^^ (_ => All)

  private def nodeTest: Parser[NodeTest] = nodeTypeTest | nodeNameTest | allNodes

  private def attribute: Parser[AxisSpecifier] = ("attribute::" | "@") ^^ (s => Attribute)

  private def descendant: Parser[AxisSpecifier] = ("descendant::" | "/") ^^
    (s => Descendant)

  private def axis: Parser[AxisSpecifier] = attribute | descendant | child

  private def combine(preds: Seq[Predicate]): Option[Predicate] = preds match {
    case Nil => None
    case pred :: Nil => Some(pred)
    case preds => Some(preds.head and combine(preds.tail).get)
  }

  private def locationStep: Parser[LocationStep] = axis ~ nodeTest ~ rep(predicate) ^^ {
    case a ~ t ~ preds => LocationStep(a, t, combine(preds))
  }

  private val slashSeparator = "/"
  def pathExpression: Parser[PathExpression] = slashSeparator ~> repsep(locationStep, slashSeparator) ^^
    (steps => PathExpression(steps))

  def parsePathExpression(expr: String): PathExpression = {
    try {
      parseTo(StringFileArtifact("<input>", expr), phrase(pathExpression))
    } catch {
      case e: IllegalArgumentException =>
        throw new IllegalArgumentException(s"Path expression '$expr' is invalid: [${e.getMessage}]", e)
    }
  }
}

/**
  * Default implementation of PathExpressionParser. Import this
  * class for default conversion from Strings to path expressions. 
  */
object PathExpressionParser extends PathExpressionParser {

  implicit def parseString(expr: String): PathExpression = PathExpressionParser.parsePathExpression(expr)

}