package com.atomist.rug.test

import com.atomist.param.ParameterValues
import com.atomist.project.ProjectOperation
import com.atomist.project.common.{InvalidParametersException, MissingParametersException}
import com.atomist.project.edit.{FailedModificationAttempt, NoModificationNeeded, ProjectEditor, SuccessfulModification}
import com.atomist.project.generate.ProjectGenerator
import com.atomist.source.{ArtifactSource, ArtifactSourceUtils}

/**
  * Log of diagnostics relating to an individual test.
  * Included in report data.
  */
class TestEventLog {

  private var _input: Option[ArtifactSource] = None
  private var params: Option[ParameterValues] = None
  private var _output: Option[ArtifactSource] = None

  def recordInput(input: ArtifactSource): Unit = _input = Some(input)

  def recordParameters(poa: ParameterValues): Unit = params = Some(poa)

  def recordOutput(output: ArtifactSource): Unit = _output = Some(output)

  def input: Option[ArtifactSource] = _input

  def output: Option[ArtifactSource] = _output

  def parameters: Option[ParameterValues] = params
}

object EmptyTestEventLog extends TestEventLog

trait ExecutionLog {

  def log(message: String)
}

/**
  * Log of test execution steps. Can be displayed to user to show
  * progress of test execution. Not retained as has no long-term significance.
  */
object ConsoleExecutionLog extends ExecutionLog {

  override def log(message: String): Unit = println(message)
}

/**
  * Execute Rug Test BDD style tests
  */
class TestRunner(executionLog: ExecutionLog = ConsoleExecutionLog) {

  import com.atomist.rug.runtime.NamespaceUtils._

  /**
    * Run the given test programs
    *
    * @param testPrograms  test programs
    * @param testResources backing archive for the tests
    * @param context       known project operations. What we're testing
    * @param namespace     current namespace for use in name resolution
    * @return a test report
    */
  def run(
           testPrograms: Seq[TestScenario],
           testResources: ArtifactSource,
           context: Seq[ProjectOperation],
           namespace: Option[String] = None): TestReport = {
    val executedTests = testPrograms.map(test => {
      resolve(test.testedOperation, namespace, context, test.imports) match {
        case None =>
          ExecutedTest.failure(test.name,
            s"Scenario '${test.name}' tests editor '${test.testedOperation}', which was not found. \n" +
              s"Known operations are [${context.map(op => op.name).mkString(",")}]", EmptyTestEventLog)
        case Some(ed: ProjectEditor) =>
          executeAgainst(test, ed, testResources)
        case Some(gen: ProjectGenerator) =>
          executeGenerator(test, gen, testResources)
        case _ => ???
      }
    })
    TestReport(executedTests)
  }

  private def executeAgainst(test: TestScenario, ed: ProjectEditor, testResources: ArtifactSource): ExecutedTest = {
    val eventLog = new TestEventLog
    try {
      // TODO should publish events rather than sysout
      executionLog.log(s"Executing scenario ${test.name}...")
      val input: ArtifactSource = test.input(testResources)
      if (test.debug) {
        eventLog.recordInput(input)
      }

      val poa: ParameterValues = test.args
      eventLog.recordParameters(poa)

      if (test.givenInvocations.nonEmpty) {
        ??? // not implemented
      }

      val applicability = ed.applicability(testResources)

      test.outcome.assertions.toList match {
        case NotApplicableAssertion :: Nil if !applicability.canApply =>
          ExecutedTest(test.name,
            Seq(TestedAssertion(result = true, s"Editor was not applicable as expected: ${applicability.message}")), eventLog)
        case NotApplicableAssertion :: Nil if applicability.canApply =>
          ExecutedTest(test.name,
            Seq(TestedAssertion(result = false, s"Editor was applicable but should not have been: ${applicability.message}")), eventLog)
        case _ =>
          val modificationAttempt = ed.modify(input, poa)
          modificationAttempt match {
            case sm: SuccessfulModification =>
              verifyOutput(test, testResources, sm.result, eventLog)
            case fm: FailedModificationAttempt =>
              test.outcome.assertions match {
                case Seq(ShouldFailAssertion) =>
                  ExecutedTest(test.name,
                    Seq(TestedAssertion(result = true, s"Editor failed as expected: ${fm.failureExplanation}")), eventLog)
                case _ => ExecutedTest.failure(test.name, s"Editor failed: ${fm.failureExplanation}", eventLog)
              }

            case nmn: NoModificationNeeded =>
              test.outcome.assertions match {
                case Seq(NoChangeAssertion) =>
                  ExecutedTest(test.name,
                    Seq(TestedAssertion(result = true, s"Editor did not modify content as expected: ${nmn.comment}")), eventLog)
                case _ => ExecutedTest.failure(test.name, s"Editor made no change: ${nmn.comment}", eventLog)
              }
          }
      }
    }
    catch {
      case mp: MissingParametersException =>
        test.outcome.assertions match {
          case Seq(ShouldBeMissingParametersAssertion) =>
            ExecutedTest(test.name,
              Seq(TestedAssertion(result = true, s"Editor was missing parameters: ${mp.getMessage}")), eventLog)
          case _ =>
            ExecutedTest.failure(test.name, s"Editor failed due to missing parameters: ${mp.getMessage}", eventLog)
        }
      case ivp: InvalidParametersException =>
        test.outcome.assertions match {
          case Seq(ShouldBeInvalidParametersAssertion) =>
            ExecutedTest(test.name, Seq(TestedAssertion(result = true, s"Editor had invalid parameters: ${ivp.getMessage}")), eventLog)
          case _ =>
            ExecutedTest.failure(test.name, s"Editor failed due to invalid parameters: ${ivp.getMessage}", eventLog)
        }
    }
  }

  private def executeGenerator(test: TestScenario, gen: ProjectGenerator, testResources: ArtifactSource): ExecutedTest = {
    val eventLog = new TestEventLog
    try {
      // TODO should publish events rather than sysout
      executionLog.log(s"Executing scenario ${test.name}...")

      val poa: ParameterValues = test.args
      eventLog.recordParameters(poa)

      if (test.givenInvocations.nonEmpty) {
        ??? // not implemented
      }

      val result = gen.generate(test.name, poa)
      verifyOutput(test, testResources, result, eventLog)
    }
    catch {
      case mp: MissingParametersException =>
        test.outcome.assertions match {
          case Seq(ShouldBeMissingParametersAssertion) =>
            ExecutedTest(test.name,
              Seq(TestedAssertion(result = true, s"Generator was missing parameters: ${mp.getMessage}")), eventLog)
          case _ =>
            ExecutedTest.failure(test.name, s"Generator failed due to missing parameters: ${mp.getMessage}", eventLog)
        }
      case ivp: InvalidParametersException =>
        test.outcome.assertions match {
          case Seq(ShouldBeInvalidParametersAssertion) =>
            ExecutedTest(test.name, Seq(TestedAssertion(result = true, s"Generator had invalid parameters: ${ivp.getMessage}")), eventLog)
          case _ =>
            ExecutedTest.failure(test.name, s"Generator failed due to invalid parameters: ${ivp.getMessage}", eventLog)
        }
    }
  }

  private def verifyOutput(test: TestScenario, testResources: ArtifactSource, outputProject: ArtifactSource, eventLog: TestEventLog): ExecutedTest = {
    if (test.debug) {
      eventLog.recordOutput(outputProject)
    }
    val assertions = test.outcome.assertions
      .map(assertion => {
        executionLog.log(s"\tTesting assertion $assertion")
        assertion.test(test, testResources, outputProject)
      })
    ExecutedTest(test.name, assertions, eventLog)
  }
}

case class TestReport(
                       tests: Seq[ExecutedTest]
                     ) {

  def passed = tests.forall(_.passed)

  def passedTests = tests.filter(_.passed)

  def failures = tests.filter(!_.passed)

  def testSummary = s"Test report: " +
    s"${passedTests.size} of ${tests.size} tests passed\n" +
    "\t" + failures.mkString("\n\t") +
    (passed match {
      case true =>
        "\nTest SUCCESS\n"
      case false =>
        "\nTest FAILURE\n"
    })

  def debugOutput: String = {
    def testDebugOutput(test: ExecutedTest): String =
      test.name + "diagnostics\n" +
        "\tInput: " + test.eventLog.input.map(i => ArtifactSourceUtils.prettyListFiles(i)).getOrElse("") +
        "\tOutput: " + test.eventLog.output.map(o => ArtifactSourceUtils.prettyListFiles(o)).getOrElse("")

    failures.map(f => testDebugOutput(f)).mkString("\n")
  }

  override def toString = testSummary + "\n" + debugOutput
}

case class ExecutedTest(
                         name: String,
                         assertions: Seq[TestedAssertion],
                         eventLog: TestEventLog
                       ) {

  def passed = assertions.forall(_.result)

  def failures = assertions.filter(!_.result)

  override def toString = s"'$name': " + (passed match {
    case true => s"${assertions.size} assertions passed:\n" +
      assertions.map(a => s"\t$a").mkString("\n")
    case false => s"${failures.size} of ${assertions.size} assertions FAILED: \n" +
      failures.map(f => s"\t$f").mkString("\n")
  })
}

object ExecutedTest {

  def failure(name: String, message: String, eventLog: TestEventLog) =
    ExecutedTest(name, Seq(TestedAssertion(result = false, message)), eventLog)
}

case class TestedAssertion(
                            result: Boolean,
                            message: String,
                            assertion: Option[Assertion] = None) {

  def this(result: Boolean, assertion: Assertion) =
    this(result, s"Assertion '$assertion' failed", Some(assertion))

  override def toString = (result match {
    case false => "failed: "
    case true => "passed: "
  }) + assertion.map(_.toString).getOrElse(message)
}