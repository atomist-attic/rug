package com.atomist.rug.runtime

/**
  * Used to describe tests on a Rug
  * @param kind what kind of test. Currently only "integration" exists and we don't yet make use of it.
  * @param description
  */
case class TestDescriptor (kind: String, description: String)
