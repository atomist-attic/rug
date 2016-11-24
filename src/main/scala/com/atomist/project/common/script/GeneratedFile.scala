package com.atomist.project.common.script

/**
  * Simplest possible Java object to create from JavaScript/etc.
  * Conceal file artifact internals, method overloading etc.
  */
case class GeneratedFile(path: String, contents: String)
