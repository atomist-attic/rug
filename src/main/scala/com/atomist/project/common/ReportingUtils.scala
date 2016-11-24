package com.atomist.project.common

/**
  * Utility methods that help in error reporting
  */
object ReportingUtils {

  /**
    * Format the fragment adding line numbers, to help with diagnostics
    *
    * @param s
    * @return
    */
  def withLineNumbers(s: String): String = {
    if (s == null) null
    else
      s.lines
        .zipWithIndex
        .map(tup => s"${tup._2 + 1} ${tup._1}")
        .mkString("\n")
  }
}
