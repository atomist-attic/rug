package com.atomist.rug.spi

/**
  * Represents some secret stored in the Atomist Secrets' Service
  *
  * @param name Name of the secret. Used as the name in the ParameterValues (if used like that)
  * @param path Path to the secret. Relative to some external context
  */

case class Secret (name: String, path: String)
