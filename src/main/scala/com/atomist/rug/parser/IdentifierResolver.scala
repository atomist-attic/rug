package com.atomist.rug.parser

/**
  * Used where we may need to resolve a well-known, global identifier
  * such as a parameter regex placeholder at parsing time.
  */
trait IdentifierResolver {

  type SourceDescription = String

  def resolve(identifier: String): Either[SourceDescription, String]
}
