package com.atomist.rug.test

import com.atomist.rug.parser.{CommonRugProductionsParser, Predicate, RunOtherOperation}
import com.atomist.source.FileArtifact

/**
  * Use Scala ParserCombinators to parse test scenario scripts
  */
object RugTestParser
  extends CommonRugProductionsParser {

  import RugTestTokens._

  override def identifierRef: Parser[IdentifierRef] =
    identifierRef(KeywordsToAvoidInBody, camelCaseIdentifier)

  private def freeText: Parser[String] = """\w(.*)+""".r

  // TODO improve this
  private def unquotedFilename: Parser[String] = """[\w|/|\.-[*]%![$]]+""".r

  private def filename: Parser[String] = literalString | unquotedFilename

  private def scenarioName: Parser[String] = ScenarioToken.r ~> freeText

  protected override def parameterName: Parser[String] = identifierRef(KeywordsToAvoidInBody, camelCaseIdentifier) ^^ (id => id.name)

  private def inlineFile: Parser[FileSpec] = filename ~ EqualsToken.r ~ literalString ^^ {
    case path ~ _ ~ content => InlineFileSpec(path, content)
  }

  private def loadedFile: Parser[FileSpec] = filename ~ FromToken.r ~ filename ^^ {
    case path ~ _ ~ content => LoadedFileSpec(path, content)
  }

  private def filesUnder: Parser[FileSpec] = FilesUnderToken ~> filename ^^ (path => FilesUnderFileSpec(path))

  private def archiveRoot: Parser[FileSpec] = ArchiveRootToken ^^ (_ => ArchiveRootFileSpec)

  private def emptyArchive: Parser[FileSpec] = EmptyToken ^^ (_ => EmptyArchiveFileSpec)

  private def fileSpec: Parser[FileSpec] = inlineFile | loadedFile | filesUnder | archiveRoot | emptyArchive

  private def givenFiles: Parser[GivenFiles] = GivenToken.r ~> rep(fileSpec) ^^ (f => GivenFiles(f))

  private def givenOperations: Parser[Seq[RunOtherOperation]] = rep(runOtherOperation)

  // We override this to disable ANDing as we want separate predicates
  override protected def predicateExpression: Parser[Predicate] = predicateTerm

  private def projectPredicateAssertion: Parser[PredicateAssertion] = predicateExpression ^^ (
    pred => PredicateAssertion(pred)
    )

  private def assertion: Parser[Assertion] = projectPredicateAssertion

  private def assertions: Parser[Seq[Assertion]] = rep1sep(assertion, AndToken)

  private def andThen: Parser[Then] = ThenToken.r ~> (assertions | NoChangeToken |
    NotApplicableToken | ShouldFailToken | MissingParameters | InvalidParameters) ^^ {
    case assertions: Seq[Assertion @unchecked] => Then(assertions)
    case NoChangeToken => Then(Seq(NoChangeAssertion))
    case NotApplicableToken => Then(Seq(NotApplicableAssertion))
    case ShouldFailToken => Then(Seq(ShouldFailAssertion))
    case MissingParameters => Then(Seq(ShouldBeMissingParametersAssertion))
    case InvalidParameters => Then(Seq(ShouldBeInvalidParametersAssertion))
  }

  private def debug: Parser[Boolean] = opt("debug" ~> EqualsToken ~> ("true" | "false")) ^^ {
    case Some("true") => true
    case _ => false
  }

  private def testProgram: Parser[TestScenario] = scenarioName ~ debug ~
    rep(uses) ~ rep(letStatement) ~
    givenFiles ~ opt(givenOperations ~ WhenToken | WhenToken) ~ runOtherOperation ~ andThen ^^ {
    case name ~ debug ~ uses ~ computations ~ gf ~ Some((go: Seq[RunOtherOperation]) ~ whenToken) ~ roo ~ andThen =>
      TestScenario(name, debug, uses, computations, gf, go, roo, andThen)
    case name ~ debug ~ uses ~ computations ~ gf ~ Some(whenToken) ~ roo ~ andThen =>
      TestScenario(name, debug, uses, computations, gf, Seq(), roo, andThen)
    case name ~ debug ~ uses ~ computations ~ gf ~ None ~ roo ~ andThen =>
      TestScenario(name, debug, uses, computations, gf, Seq(), roo, andThen)


  }

  private def testPrograms: Parser[Seq[TestScenario]] = phrase(rep1(testProgram))

  def parse(f: FileArtifact): Seq[TestScenario] =
    parseTo(f, testPrograms)

}
