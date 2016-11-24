package com.atomist.rug.parser

import com.atomist.param.{AllowedValue, Parameter, Tag}
import com.atomist.rug._
import com.atomist.scalaparsing.ScriptBlock
import com.atomist.source.FileArtifact

/**
  * Use Scala parser combinator to Parse Rug scripts.
  */
class ParserCombinatorRugParser(
                                 identifierResolver: IdentifierResolver = DefaultIdentifierResolver
                               )
  extends CommonRugProductionsParser
    with RugParser {

  import RugParser._

  private def regexParamPattern: Parser[String] = withCustomErrorHandling(
    (identifierLookup | literalString | regexp) ^^ {
      case il: IdentifierLookup =>
        identifierResolver.resolve(il.id) match {
          case Left(sourceOfValidIdentifiers) =>
            throw new CustomizedParseFailureException(s"Cannot resolve well-known regex in parameter type '${il.id}. Consider adding it to $sourceOfValidIdentifiers", 1)
          case Right(pat) =>
            pat
        }
      case s: String => s
    })

  private def paramPattern: Parser[ParameterPattern] = (stringArray | regexParamPattern) ^^ {
    case p: String => RegexParameterPattern(p)
    case values: Seq[String] => AllowedValuesParameterPattern(values)
  }

  protected override def parameterName: Parser[String] = identifierRef(ReservedWordsToAvoidInBody, camelCaseIdentifier) ^^ (id => id.name)

  sealed trait ParameterPattern

  case class RegexParameterPattern(pattern: String) extends ParameterPattern

  case class AllowedValuesParameterPattern(allowedValues: Seq[String]) extends ParameterPattern

  case class ParameterDef(name: String, annotations: Seq[Annotation], pattern: ParameterPattern)

  private def parameter: Parser[ParameterDef] = rep(annotation) ~ ParameterToken.r ~
    parameterName ~ ColonToken ~ paramPattern ^^ {
    case annotations ~ _ ~ name ~ _ ~ pattern => ParameterDef(name, annotations, pattern)
  }

  sealed trait Op

  object Editor extends Op

  object Reviewer extends Op

  object Executor extends Op

  object Predicate extends Op

  case class OperationSpec(
                            op: Op,
                            name: String,
                            tags: Seq[String],
                            description: String,
                            publishedName: Option[String] = None,
                            imports: Seq[Import]
                          )

  private def operationSpec(operationToken: String): Parser[OperationSpec] =
    rep(annotation) ~ operationToken ~ capitalizedIdentifier ~ rep(uses) ^^ {
      case annotations ~ op ~ name ~ imports =>
        val o: Op = op match {
          case EditorToken => Editor
          case PredicateToken => Predicate
          case ReviewerToken => Reviewer
          case ExecutorToken => Executor
        }
        var ed = OperationSpec(o, name, Nil, name, None, imports)
        for (annotation <- annotations) annotation match {
          case Annotation(DescriptionAnnotationAttribute, Some(desc: String)) => ed = ed.copy(description = desc)
          case Annotation(TagAnnotation, Some(tag: String)) => ed = ed.copy(tags = ed.tags :+ tag)
          case Annotation(GeneratorAnnotation, pubName: Option[String]) =>
            ed = ed.copy(publishedName = Some(pubName.getOrElse(ed.name)))
          // TODO Do what we did for parameters
          // and do the resolution later, because someone thought this makes it worse, but it doesn't, printing the $op helps
          case x => throw new CustomizedParseFailureException(s"Annotation $x not allowed on operation $op", 1)
        }
        ed
    }

  private def withToken: Parser[String] = WithToken.r

  private def withBlock(simpleAction: Parser[DoStep]): Parser[With] =
    withToken ~> selection ~ doSteps(simpleAction) ^^ {
      case selection ~ dos => With(selection.kind, selection.alias, selection.constructionInfo, selection.predicate, dos)
    }

  // Pass in the allowed simple do step type
  private def action(simpleAction: Parser[DoStep]): Parser[Action] =
    withBlock(simpleAction) | runOtherOperation

  override def identifierRef: Parser[IdentifierRef] = identifierRef(ReservedWordsToAvoidInBody, camelCaseIdentifier)

  private def doDoStep(): Parser[DoStep] = DoToken ~> functionInvocation ~ rep(functionArg) <~ opt(terminator) ^^ {
    case function ~ args => FunctionDoStep(function.function, function.target, args, function.pathBelow)
  }

  private def doSteps(simpleAction: Parser[DoStep]): Parser[Seq[DoStep]] =
    (simpleAction | compoundDoStep) ^^ {
      case d: DoStep => Seq(d)
      case dos: Seq[DoStep] => dos
    }

  private def simpleWithDoStep: Parser[DoStep] = (doDoStep | withBlock(simpleWithDoStep)) ^^ {
    case d: DoStep => d
    case w: With => WithDoStep(w)
  }

  private def compoundDoStep: Parser[Seq[DoStep]] = BeginToken ~> rep1(simpleWithDoStep) <~ EndToken

  private def successBlock: Parser[SuccessBlock] = SuccessToken ~> literalString ^^ (message => SuccessBlock(message))

  private def precondition: Parser[Condition] = PreconditionToken ~> capitalizedIdentifier ^^ (name => Condition(name))

  private def postcondition: Parser[Condition] = PostconditionToken ~> capitalizedIdentifier ^^ (name => Condition(name))

  private def paramDefsToParameters(opName: String, parameterDefs: Seq[ParameterDef]): Seq[Parameter] = {
    for (pd <- parameterDefs) yield {
      var parm = pd.pattern match {
        case rp: RegexParameterPattern => Parameter(pd.name, rp.pattern)
        case avp: AllowedValuesParameterPattern => new Parameter(pd.name).setAllowedValues(
          avp.allowedValues.map(av => AllowedValue(av, av))
        )
      }
      for (annotation <- pd.annotations) annotation match {
        case Annotation(OptionalAnnotationAttribute, None) => parm = parm.setRequired(false)
        case Annotation(TagAnnotation, Some(tag: String)) => parm = parm.tagWith(Tag(tag, tag))
        case Annotation(DescriptionAnnotationAttribute, Some(desc: String)) => parm = parm.describedAs(desc)
        case Annotation(DefaultAnnotationAttribute, Some(deft: String)) => parm = parm.setDefaultValue(deft).setRequired(false)
        case Annotation(DefaultRefAnnotationAttribute, Some(deft: String)) => parm = parm.setDefaultRef(deft)
        case Annotation(ValidInputAnnotationAttribute, Some(s: String)) => parm = parm.setValidInputDescription(s)
        case Annotation(HideAnnotationAttribute, None) => parm = parm.setDisplayable(false)
        case Annotation(MinLengthAnnotationAttribute, Some(n: Int)) => parm.setMinLength(n)
        case Annotation(MaxLengthAnnotationAttribute, Some(n: Int)) => parm.setMaxLength(n)
        case Annotation(DisplayNameAnnotationAttribute, Some(name: String)) => parm = parm.setDisplayName(name)
        case ann => throw new InvalidRugAnnotationValueException(opName, ann)
      }
      parm
    }
  }

  protected def scriptActionBlock: Parser[ScriptBlock] = javaScriptBlock

  private def opActions: Parser[Seq[Action]] = (scriptActionBlock | rep1(action(simpleWithDoStep))) ^^ {
    case sb: ScriptBlock => Seq(ScriptBlockAction(sb))
    case actions: Seq[Action] => actions
  }

  private def rugEditor: Parser[RugEditor] =
    operationSpec(EditorToken) ~
      rep(precondition) ~ opt(postcondition) ~
      rep(parameter) ~ rep(letStatement) ~
      opActions ~ opt(successBlock) ^^ {
      case opSpec ~ preconditions ~ postcondition ~ params ~ compBlock ~ actions ~ success =>
        RugEditor(opSpec.name, opSpec.publishedName, opSpec.tags, opSpec.description, opSpec.imports,
          preconditions, postcondition,
          paramDefsToParameters(opSpec.name, params),
          compBlock, actions, success)
    }

  private def rugReviewer: Parser[RugReviewer] =
    operationSpec(ReviewerToken) ~
      rep(parameter) ~ rep(letStatement) ~
      rep1(action(simpleWithDoStep)) ^^ {
      case opSpec ~ params ~ compBlock ~ actions =>
        RugReviewer(opSpec.name, opSpec.publishedName, opSpec.tags, opSpec.description, opSpec.imports,
          paramDefsToParameters(opSpec.name, params),
          compBlock, actions)
    }

  object TrueDoStep extends ToEvaluateDoStep(TruePredicate)

  private def predicateWithBlock: Parser[With] =
    withToken ~> selection ~ opt(predicateWithBlock) ^^ {
      case selection ~ None =>
        With(selection.kind, selection.alias, selection.constructionInfo, selection.predicate,
          Seq(TrueDoStep))
      case selection ~ Some(w) =>
        With(selection.kind, selection.alias, selection.constructionInfo, selection.predicate,
          Seq(WithDoStep(w)))
    }

  private def rugPredicate: Parser[RugProjectPredicate] =
    operationSpec(PredicateToken) ~
      rep(parameter) ~ rep(letStatement) ~
      predicateWithBlock ^^ {
      case opSpec ~ params ~ compBlock ~ predicateWithBlock =>
        RugProjectPredicate(opSpec.name, opSpec.publishedName, opSpec.tags, opSpec.description, opSpec.imports,
          paramDefsToParameters(opSpec.name, params),
          compBlock, Seq(predicateWithBlock))
    }

  // TODO permit compound do steps also
  private def executionEditorCallOnSuccess: Parser[DoStep] = OnSuccessToken ~> simpleWithDoStep

  private def executionEditorCallOnFail: Parser[DoStep] = OnFailToken ~> simpleWithDoStep

  private def executionEditorCallOnNoChange: Parser[DoStep] = OnNoChangeToken ~> simpleWithDoStep

  private def executionEditorCallDoStep: Parser[RunOtherOperation] =
    EditCallToken ~> capitalizedIdentifier ~ rep(functionArg) ~ opt(executionEditorCallOnSuccess) ~ opt(executionEditorCallOnNoChange) ~ opt(executionEditorCallOnFail) ^^ {
      case id ~ args ~ success ~ noChange ~ failure =>
        new RunOtherOperation(id, args, success, noChange, failure) with EditorFlag
    }

  private def executionReviewerCallDoStep: Parser[RunOtherOperation] =
    ReviewCallToken ~> capitalizedIdentifier ~ rep(functionArg) ^^ {
      case id ~ args => new RunOtherOperation(id, args, None, None, None) with ReviewerFlag
    }

  private def simpleExecutionDoStep: Parser[DoStep] =
    (executionEditorCallDoStep | executionReviewerCallDoStep | withBlock(simpleExecutionDoStep)) ^^ {
      case d: DoStep => d
      case w: With => WithDoStep(w)
    }

  protected def executorActions: Parser[Seq[Action]] = (scriptActionBlock | rep1(action(simpleExecutionDoStep))) ^^ {
    case sab: ScriptBlock => Seq(ScriptBlockAction(sab))
    case actions: Seq[Action] => actions
  }

  private def rugExecutor: Parser[RugExecutor] =
    operationSpec(ExecutorToken) ~
      rep(parameter) ~ rep(letStatement) ~
      executorActions ^^ {
      case opSpec ~ params ~ compBlock ~ actions =>
        RugExecutor(opSpec.name, opSpec.publishedName, opSpec.tags, opSpec.description, opSpec.imports,
          paramDefsToParameters(opSpec.name, params),
          compBlock, actions)
    }

  protected def rugProgram: Parser[RugProgram] = rugPredicate | rugEditor | rugReviewer | rugExecutor

  private def rugPrograms: Parser[Seq[RugProgram]] = phrase(rep1(rugProgram))

  override def parse(f: FileArtifact): Seq[RugProgram] = parseTo(f, rugPrograms)
}
