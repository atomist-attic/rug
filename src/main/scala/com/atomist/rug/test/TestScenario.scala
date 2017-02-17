package com.atomist.rug.test

import com.atomist.param.{Parameter, ParameterValues, SimpleParameterValues}
import com.atomist.project.ProjectOperation
import com.atomist.rug.kind.DefaultTypeRegistry
import com.atomist.rug.parser.{Computation, Predicate, RunOtherOperation}
import com.atomist.rug.runtime.rugdsl.{DefaultEvaluator, DefaultViewFinder, RugOperationSupport}
import com.atomist.rug.{EmptyRugFunctionRegistry, Import}
import com.atomist.source._

/**
  * Represents a BDD style test scenario
  *
  * @param name             Name of the scenario. Natural text
  * @param givenFiles            given
  * @param editorInvocation editor and parameters to invoke it
  * @param outcome          assertions about the result of invoking the editor
  */
case class TestScenario(
                         name: String,
                         debug: Boolean,
                         imports: Seq[Import],
                         computations: Seq[Computation],
                         givenFiles: GivenFiles,
                         givenInvocations: Seq[RunOtherOperation],
                         editorInvocation: RunOtherOperation,
                         outcome: Then
                       ) extends RugOperationSupport {

  override val kindRegistry = DefaultTypeRegistry

  override val evaluator = new DefaultEvaluator(new EmptyRugFunctionRegistry)

  override def namespace: Option[String] = None

  override protected def operations: Seq[ProjectOperation] = ???

  // Be consistent with types of generator-lib
  override def parameters: Seq[Parameter] = Nil

  /**
    *
    * @return what editor are we testing
    */
  def testedOperation: String = editorInvocation.name

  def identifierMap(backingArchive: ArtifactSource): Map[String, Object] = {
    val context = null
    val project = input(backingArchive)
    buildIdentifierMap(backingArchive, args)
  }

  /**
    *
    * @return ArtifactSource created from given block
    */
  def input(testBacking: ArtifactSource): ArtifactSource = {
    val executeAgainst = new SimpleFileBasedArtifactSource("test target",
      givenFiles.fileSpecs.flatMap(_.toFiles(testBacking)))
    executeAgainst
  }

  /**
    *
    * @return ProjectOperationArguments created from editor invocation
    */
  def args: ParameterValues = {
    parametersForOtherOperation(editorInvocation, SimpleParameterValues.Empty)
  }
}

case class GivenFiles(fileSpecs: Seq[FileSpec])

/**
  * Extract files from a backing artifact source
  */
trait FileSpec {

  /**
    * Extract files from a backing store
    *
    * @param testBacking root of backing Rug archive
    * @return a list of FileArtifacts
    */
  def toFiles(testBacking: ArtifactSource): Seq[FileArtifact]
}

case class InlineFileSpec(path: String, content: String) extends FileSpec {

  override def toFiles(testBacking: ArtifactSource) = Seq(StringFileArtifact(path, content))
}

case class LoadedFileSpec(path: String, pathInTestResources: String) extends FileSpec {

  override def toFiles(testBacking: ArtifactSource) =
    Seq(testBacking
      .findFile(pathInTestResources)
      .map(_.withPath(path))
      .getOrElse(
        throw new IllegalStateException(s"Invalid test: Cannot load test backing file '$pathInTestResources'")
      ))
}

/**
  * Load all the files from the archive
  */
object ArchiveRootFileSpec extends FileSpec {

  override def toFiles(testBacking: ArtifactSource): Seq[FileArtifact] = {
    testBacking.allFiles
  }
}

object EmptyArchiveFileSpec extends FileSpec {

  override def toFiles(testBacking: ArtifactSource): Seq[FileArtifact] = Nil
}

/**
  * Contents of the given path within the backing archive.
  */
case class FilesUnderFileSpec(directoryPath: String) extends FileSpec {

  override def toFiles(testBacking: ArtifactSource) = {
    val files = testBacking / directoryPath
    if (files.totalFileCount == 0)
      throw new IllegalStateException(
        s"Invalid test: No files in test backing directory '$directoryPath'")
    files.allFiles
  }
}

case class Then(assertions: Seq[Assertion])

sealed trait Assertion {

  def test(test: TestScenario, testBacking: ArtifactSource, output: ArtifactSource): TestedAssertion
}

object NoChangeAssertion extends Assertion {

  override def test(test: TestScenario, testBacking: ArtifactSource, output: ArtifactSource): TestedAssertion =
    new TestedAssertion(output == test.input(testBacking), this)

  override def toString = "NoChange: Output same as input"
}

// TODO Russ Change to report respected preconditions?
object NotApplicableAssertion extends Assertion {

  override def test(test: TestScenario, testBacking: ArtifactSource, output: ArtifactSource): TestedAssertion =
    new TestedAssertion(false, this)

  override def toString = "NotApplicable: Editor failed due to Preconditions not being met"
}

object ShouldFailAssertion extends Assertion {

  // This shouldn't get executed. If it does it's a failure.
  override def test(test: TestScenario, testBacking: ArtifactSource, output: ArtifactSource): TestedAssertion =
    new TestedAssertion(false, this)

  override def toString = "ShouldFail: Editor should have failed"
}

object ShouldBeMissingParametersAssertion extends Assertion {

  // This shouldn't get executed. If it does it's a failure.
  override def test(test: TestScenario, testBacking: ArtifactSource, output: ArtifactSource): TestedAssertion =
    new TestedAssertion(false, this)

  override def toString = "ShouldBeMissingParameters: Editor should have failed"
}

object ShouldBeInvalidParametersAssertion extends Assertion {

  // This shouldn't get executed. If it does it's a failure.
  override def test(test: TestScenario, testBacking: ArtifactSource, output: ArtifactSource): TestedAssertion =
    new TestedAssertion(false, this)

  override def toString = "ShouldBeInvalidParameters: Editor should have failed"
}

case class PredicateAssertion(
                               predicate: Predicate
                             )
  extends Assertion {

  override def test(test: TestScenario, testBacking: ArtifactSource, output: ArtifactSource): TestedAssertion = {
    val pa = DefaultViewFinder
    val result = pa.invokePredicate(EmptyArtifactSource(""), test.args,
      test.identifierMap(testBacking), predicate, PredicateAssertion.ProjectResultName, new ProjectAssertions(output))
    new TestedAssertion(result, this)
  }

  override def toString = predicate.toString
}

object PredicateAssertion {

  /**
    * Name of the generated project as a variable in a test predicate.
    */
  val ProjectResultName = "result"
}
