package com.atomist.project.review

object Severity extends Enumeration(initial = 0) {
  type Severity = Value
  val FINE, POLISH, MAJOR, BROKEN = Value
}
