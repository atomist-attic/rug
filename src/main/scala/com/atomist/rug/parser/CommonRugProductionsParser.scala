package com.atomist.rug.parser

import com.atomist.model.content.text.PathExpressionParser
import com.atomist.rug.Import
import com.atomist.util.scalaparsing._

/**
  * Parsing superclass shared between Rug and Rug test parsing
  * that understands function invocations and operation invocations
  */
abstract class CommonRugProductionsParser extends PathExpressionParser with CommonRugTokens {

  import RugParser._

  /** We allow C style block comments and Python style # to end of line comments */
  override protected val whiteSpace = CBlockCommentAndHashLineCommentWhitespace

  private def qualifiedOperation: Parser[String] = """^([a-z][a-zA-Z-_$\d]*\.)*[A-Z][a-zA-Z_$\d]*""".r

  protected def uses: Parser[Import] = usesToken ~> qualifiedOperation ^^ (fqn => Import(fqn))

  def literalString: Parser[String] = tripleQuotedString | doubleQuotedString | singleQuotedString

  def literalInt: Parser[Int] = wholeNumber ^^ (n => n.toInt)

  def literalDouble: Parser[Double] = floatingPointNumber ^^ (s => s.toDouble)

  def literalBoolean: Parser[Boolean] = (TrueToken | FalseToken) ^^ {
    case TrueToken => true
    case FalseToken => false
  }

  def stringArray: Parser[Seq[String]] = "[" ~> repsep(literalString, CommaToken) <~ "]"

  def literalValue: Parser[Any] =  literalString | literalInt | literalDouble | literalBoolean

  def preposition: Parser[String] = oneOf(Prepositions)

  def identifierRef: Parser[IdentifierRef]

  private def functionTarget: Parser[String] =
    identifierRef(ReservedWordsToAvoidInBody, camelCaseIdentifier) <~ DotToken ^^ (id => id.name)

  def functionInvocation: Parser[FunctionInvocation] =
    opt(functionTarget) ~
      identifierRef(ReservedWordsToAvoidInBody, camelCaseIdentifier) ~
      opt(DotToken ~> repsep(camelCaseIdentifier, DotToken)) ^^ {
      case target ~ name ~ pathElements =>
        SimpleFunctionInvocation(name.name, Nil, target, pathElements.getOrElse(Nil))
    }

  private def annotationToken: Parser[String] = AtToken

  protected def annotation: Parser[Annotation] = annotationToken ~ ident ~ opt(literalValue) ^^ {
    case _ ~ name ~ value => Annotation(name, value)
  }

  protected def parameterName: Parser[String]

  protected def pathExpressionBlock: Parser[PathExpressionValue] = "$(" ~ pathExpression ~ ")" ~
    opt("." ~> identifierRefString(ReservedWordsToAvoidInBody, ident)) ^^ {
    case _ ~ pe ~ _ ~ prop => PathExpressionValue(pe, prop)
  }

  protected def letStatement: Parser[Computation] =
    letToken ~> parameterName ~ EqualsToken ~ (pathExpressionBlock | grammarBlock | evaluation) ^^ {
      case name ~ _ ~ te => Computation(name, te)
    }

  // With references can be namespaced
  private def selectedType: Parser[String] = javaPackage

  protected def selectionAlias: Parser[(String, Option[String], String)] =
    selectedType ~ opt("(" ~> ident <~ ")") ~ opt(identifierRef(ReservedWordsToAvoidInBody, ident)) ^^ {
      case kind ~ ref ~ Some(alias) => (kind, ref, alias.name)
      case kind ~ ref ~ None =>
        // If it's an FQN take just the name
        val alias = kind.split("\\.").last
        (kind, ref, alias)
    }

  protected def whenCondition: Parser[Predicate] = opt(WhenToken ~> predicateExpression) ^^ {
    case Some(p) => p
    case None => TruePredicate
  }

  protected case class Selection(
                                  kind: String,
                                  alias: String,
                                  constructionInfo: Option[String],
                                  predicate: Predicate
                                )

  protected def selection: Parser[Selection] =
    selectionAlias ~ whenCondition <~ opt(terminator) ^^ {
      case w ~ p => Selection(w._1, w._3, w._2, p)
    }

  private def fromSelection: Parser[Selection] = fromToken ~> selection

  private def defaultValue: Parser[ToEvaluate] = evaluationNonFunctionCall

  protected def functionArg: Parser[FunctionArg] =
    opt(preposition) ~> opt(identifierRef <~ EqualsToken) ~ (evaluationNonFunctionCall | bracketedEvaluation) ^^ {
      case n ~ (te: ToEvaluate) => FunctionArg(te, n.map(_.name))
    }

  private def functionCall: Parser[ParsedRegisteredFunctionPredicate] =
    functionInvocation ~ rep(functionArg) ^^ {
      case function ~ args => ParsedRegisteredFunctionPredicate(function.function, args, function.target, function.pathBelow)
    }

  private def evaluationNonFunctionCall: Parser[ToEvaluate] =
    (literalValue | javaScriptBlock | identifierRef) ^^ {
      case te: ToEvaluate => te
      case idf: IdentifierRef => IdentifierFunctionArg(idf.name)
      case l => SimpleLiteral(l)
    }

  private def rawEvaluation: Parser[ToEvaluate] = functionCall | evaluationNonFunctionCall

  private def bracketedEvaluation: Parser[ToEvaluate] = "\\(".r ~> rawEvaluation <~ "\\)".r

  private def evaluation: Parser[ToEvaluate] = rawEvaluation | bracketedEvaluation

  private def testOperator: Parser[String] = EqualsToken

  private def comparison: Parser[Predicate] = evaluation ~ testOperator ~ evaluation ^^ {
    case f ~ EqualsToken ~ s => EqualsExpression(f, s)
  }

  protected def predicateTerm: Parser[Predicate] =
    (literalBoolean | comparison | functionCall | javaScriptBlock) ^^ {
      case fn: Predicate => fn
      case js: JavaScriptBlock => ParsedJavaScriptFunction(js)
      case true => TruePredicate
      case false => FalsePredicate
    }

  case class Term(right: Predicate, term: String) {
    def toExpression(left: Predicate) = term match {
      case "AND" => AndExpression(left, right)
      case "OR" => OrExpression(left, right)
    }
  }

  protected def andedTerm: Parser[Term] = AndToken ~> predicateExpression ^^ (right => Term(right, "AND"))

  protected def oredTerm: Parser[Term] = OrToken ~> predicateExpression ^^ (right => Term(right, "OR"))

  protected def additionalTerm: Parser[Term] = andedTerm | oredTerm

  protected def predicateExpression: Parser[Predicate] = opt(predicateTerm ~ opt(additionalTerm)) ^^ {
    case Some(ff ~ None) => ff
    case Some(a ~ Some(term)) => term.toExpression(a)
    case None => TruePredicate
  }

  protected def namedArguments: Parser[Seq[FunctionArg]] = repsep(functionArg, CommaToken)

  protected def runOtherOperation: Parser[RunOtherOperation] = capitalizedIdentifier ~ namedArguments ^^ {
    case name ~ args =>
      RunOtherOperation(name, args, None, None, None)
  }
}

case class Annotation(name: String, value: Option[Any])
