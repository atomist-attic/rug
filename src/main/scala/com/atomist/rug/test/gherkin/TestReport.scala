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
    .filter(_.result.isInstanceOf[NotYetImplemented])

  def testSummary: String = s"Test report: " +
    s"${passedTests.size} of ${archiveTestResult.testCount} tests passed\n" +
    "\t" + failures.map(f => f.feature.getName + s": FAILED (${f.result.message})").mkString("\n\t") +
    "\t" + notYetImplemented.map(f => f.feature.getName + ": Not yet implemented").mkString("\n\t") +
    (archiveTestResult.result match {
      case Passed =>
        "\nTest SUCCESS\n"
      case Failed(_,_) =>
        "\nTest FAILURE\n"
      case NotYetImplemented(s) =>
        s"\nTest NOT YET IMPLEMENTED: $s\n"
    })

  override def toString: String = testSummary

}
