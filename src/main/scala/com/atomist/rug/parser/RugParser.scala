package com.atomist.rug.parser

import java.util.Objects

import com.atomist.rug._
import com.atomist.scalaparsing.{JavaScriptBlock, Literal, ScriptBlock, ToEvaluate}
import com.atomist.source.{FileArtifact, StringFileArtifact}
import com.atomist.util.{Visitor, Visitable}

/**
  * Interface for parsing Rug files into AST.
  */
trait RugParser {

  @throws[IllegalArgumentException]
  def parse(input: String): Seq[RugProgram] = {
    parse(StringFileArtifact(RugParser.DefaultRugPath, input))
  }

  @throws[IllegalArgumentException]
  def parse(f: FileArtifact): Seq[RugProgram]
}

/**
  * Tokens and other constants relating to all possible Rug implementations.
  */
object RugParser extends CommonRugTokens {

  /**
    * Default value shown for path when we pass a string to the Rug Compiler,
    * rather than a FileArtifact.
    */
  val DefaultRugPath = "<string input>"

  val AtToken = "@"

  val CommaToken = ","

  val DotToken = "."

  val ColonToken = ":"

  val EditorToken = "editor"

  val ReviewerToken = "reviewer"

  val PredicateToken = "predicate"

  val ExecutorToken = "executor"

  val ParameterToken = "param"

  val PreconditionToken = "precondition"

  val PostconditionToken = "postcondition"

  val WithToken = "with"

  val WhenToken = "when"

  val BeginToken = "begin"

  val EndToken = "end"

  val EditCallToken = "editWith"

  val ReviewCallToken = "reviewWith"

  val DoToken = "do"

  val SuccessToken = "success"

  // TODO should this be "on success"?
  val OnSuccessToken = "onSuccess"

  val OnFailToken = "onFailure"

  val OnNoChangeToken = "onNoChange"

  /**
    * Used to indicate to indicate that an editor or reviewer should be exposed
    * directly to users
    */
  val GeneratorAnnotation = "generator"

  val TagAnnotation = "tag"

  val OptionalAnnotationAttribute = "optional"

  val DescriptionAnnotationAttribute = "description"

  val DefaultAnnotationAttribute = "default"

  val DefaultRefAnnotationAttribute = "defaultRef"

  val ValidInputAnnotationAttribute = "validInput"

  val HideAnnotationAttribute = "hide"

  val MinLengthAnnotationAttribute = "minLength"

  val MaxLengthAnnotationAttribute = "maxLength"

  val DisplayNameAnnotationAttribute = "displayName"

  /**
    * Reserved words we can use harmlessly between function arguments to
    * make programs more readable. E.g.
    * rename from "foo" to "bar"
    */
  val Prepositions = Set("to", "from")

  val ReservedWordsToAvoidInBody = CommonReservedWords ++ Set(
    EditorToken, ReviewerToken, ExecutorToken, PredicateToken,
    OnFailToken,
    EditCallToken, ReviewCallToken,
    PreconditionToken, PostconditionToken,
    ParameterToken,
    WhenToken, WithToken,
    BeginToken, EndToken,
    DoToken,
    SuccessToken,
    OnSuccessToken, OnFailToken, OnNoChangeToken
  ) ++ Prepositions
}

trait FunctionInvocation extends ToEvaluate {

  def function: String

  /**
    * Arguments. Not yet evaluated.
    */
  def args: Seq[FunctionArg]

  /**
    * Object to invoke the function on if not default target for current scope.
    */
  def target: Option[String]

  /**
    * Path elements below target to invoke on if specified, e.g. "size" in "f.contents.size".
    */
  def pathBelow: Seq[String]
}

case class SimpleFunctionInvocation(
                                     function: String,
                                     args: Seq[FunctionArg] = Nil,
                                     target: Option[String] = None,
                                     pathBelow: Seq[String] = Nil)
  extends FunctionInvocation {

  override def accept(v: Visitor, depth: Int): Unit = {
    v.visit(this, depth)
    args.foreach(a => a.accept(v, depth + 1))
  }

}

trait Predicate extends ToEvaluate

object TruePredicate extends Predicate with Literal[Boolean] {

  override val value: Boolean = true
}

object FalsePredicate extends Predicate with Literal[Boolean] {

  override val value: Boolean = false
}

case class ParsedRegisteredFunctionPredicate(
                                              function: String,
                                              args: Seq[FunctionArg] = Nil,
                                              target: Option[String] = None,
                                              pathBelow: Seq[String] = Nil)
  extends Predicate with FunctionInvocation {

  override def accept(v: Visitor, depth: Int): Unit = {
    v.visit(this, depth)
    args.foreach(a => v.visit(a, depth + 1))
  }

  override def toString = s"$function(${args.mkString(",")})"
}

case class ParsedJavaScriptFunction(js: JavaScriptBlock) extends Predicate {

  override def accept(v: Visitor, depth: Int): Unit = {
    v.visit(this, depth)
    js.accept(v, depth + 1)
  }
}

case class SuccessBlock(message: String)

trait ComparisonPredicate extends Predicate {

  def a: ToEvaluate

  def b: ToEvaluate

  override def accept(v: Visitor, depth: Int): Unit = {
    v.visit(this, depth)
    a.accept(v, depth + 1)
    b.accept(v, depth + 1)
  }
}

case class EqualsExpression(a: ToEvaluate, b: ToEvaluate) extends ComparisonPredicate

case class AndExpression(a: Predicate, b: Predicate) extends ComparisonPredicate

case class OrExpression(a: Predicate, b: Predicate) extends ComparisonPredicate

sealed trait DoStep extends Action

/**
  * Represents a do step that is a function call.
  *
  * @param function function to call
  * @param target target to call it on, if not default target in current scope
  * @param args arguments
  * @param pathBelow path below the target if specified, such as f.content.reverse
  */
case class FunctionDoStep(
                           function: String,
                           target: Option[String] = None,
                           args: Seq[FunctionArg] = Nil,
                           pathBelow: Seq[String] = Nil)
  extends DoStep with FunctionInvocation {

  override def accept(v: Visitor, depth: Int): Unit = {
    v.visit(this, depth)
    args.foreach(a => a.accept(v, depth + 1))
  }
}

case class WithDoStep(wth: With)
  extends DoStep {
  override def accept(v: Visitor, depth: Int): Unit = {
    v.visit(this, depth)
    wth.accept(v, depth + 1)
  }
}

sealed trait Action extends Visitable

/** Can be mixed into RunOtherOperation */
trait ReviewerFlag
trait EditorFlag

case class RunOtherOperation(
                              name: String,
                              args: Seq[FunctionArg],
                              success: Option[DoStep],
                              noChange: Option[DoStep],
                              failure: Option[DoStep]
                            )
  extends Action with DoStep {

  override def accept(v: Visitor, depth: Int): Unit = {
    v.visit(this, depth)
    args.foreach(a => a.accept(v, depth + 1))
    noChange.foreach(a => a.accept(v, depth + 1))
    success.foreach(a => a.accept(v, depth + 1))
    failure.foreach(a => a.accept(v, depth + 1))
  }
}

/**
  * Extended by a single action that is entirely coded in another language,
  * such as Clojure or JavaScript.
  */
case class ScriptBlockAction(scriptBlock: ScriptBlock) extends Action {

  override def accept(v: Visitor, depth: Int): Unit = {
    v.visit(this, depth)
    scriptBlock.accept(v, depth + 1)
  }
}

sealed trait Selected {

  val kind: String

  val alias: String

  /**
    * Identifier carrying info used to create the type instances.
    */
  val constructionInfoIdentifier: Option[String]

  val predicate: Predicate
}

case class With(
                 kind: String,
                 alias: String,
                 constructionInfoIdentifier: Option[String],
                 predicate: Predicate,
                 doSteps: Seq[DoStep]
               )
  extends Selected with Action {
  override def accept(v: Visitor, depth: Int): Unit = {
    v.visit(this, depth)
    predicate.accept(v, depth + 1)
    doSteps.foreach(d => d.accept(v, depth + 1))
  }
}


case class ToEvaluateDoStep(te: ToEvaluate) extends DoStep {

  override def accept(v: Visitor, depth: Int): Unit = {
    v.visit(this, depth)
    te.accept(v, depth + 1)
  }
}

trait FunctionArg extends ToEvaluate {

  def parameterName: Option[String]

  override def accept(v: Visitor, depth: Int): Unit = v.visit(this, depth)
}

object FunctionArg {

  def apply(te: ToEvaluate, parameterName: Option[String]) = te match {
    case fa: FunctionArg if fa.parameterName == parameterName => fa
    case _ => WrappedFunctionArg(te, parameterName)
  }
}

case class WrappedFunctionArg(te: ToEvaluate, parameterName: Option[String] = None)
  extends FunctionArg {

  override def accept(v: Visitor, depth: Int): Unit = {
    v.visit(this, depth)
    te.accept(v, depth + 1)
  }

  override def toString = Objects.toString(te)
}

case class IdentifierFunctionArg(name: String, parameterName: Option[String] = None)
  extends FunctionArg

case class JavaScriptFunctionArg(js: JavaScriptBlock, parameterName: Option[String] = None) extends FunctionArg

case class Computation(
                        name: String,
                        te: ToEvaluate
                      )