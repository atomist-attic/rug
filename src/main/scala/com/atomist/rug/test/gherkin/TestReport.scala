package com.atomist.rug.test.gherkin

/**
  * Convenient class exposing a report
  */
class TestReport(archiveTestResult: ArchiveTestResult) {

  def passed: Boolean = archiveTestResult.result == Passed

  def passedTests: Seq[FeatureResult] = archiveTestResult.featureResults
    .filter(_.passed)

  def failures: Seq[FeatureResult] = archiveTestResult.featureResults
      .filter(_.result.isInstanceOf[Failed])

  def notYetImplemented: Seq[FeatureResult] = archiveTestResult.featureResults
    .filter(_.result == NotYetImplemented)

  def testSummary: String = s"Test report: " +
    s"${passedTests.size} of ${archiveTestResult.testCount} tests passed\n" +
    "\t" + failures.mkString("\n\t") +
    "\t" + notYetImplemented.mkString("\n\t") +
    (archiveTestResult.result match {
      case Passed =>
        "\nTest SUCCESS\n"
      case Failed(x) =>
        "\nTest FAILURE\n"
      case NotYetImplemented =>
        "\nTest NOT YET IMPLEMENTED\n"
    })

}
