package com.atomist.rug.kind.grammar

/**
  * Sometimes parsing a file's contents into nodes is easier
  * with a little massaging.
  *
  * For instance, Elm is whitespace sensitive, so I mark
  * the sensitive bits of whitespace with tokens.
  * Then we strip those tokens in the postprocess step,
  * before writing the code back out to the file.
  *
  * Implement this interface to perform pre- and post- parsing
  * steps on your file type.
  */
trait TextPreprocessor {
  /**
    * This method returns the version of file contents that
    * the parsed nodes work with.
    *
    */
  def preProcess(content: String): String

  /**
    * In case you need to add markup before parsing,
    * you probably need to remove it before writing
    * back to the file.
    *
    * This goes from the parsed nodes' version of the
    * contents to the file's real contents.
    *
    * It can operate on any prefix of
    * the file contents.
    */
  def postProcess(content: String): String

}

object IdentityTextPreprocessor extends TextPreprocessor {
  def preProcess(content: String): String = content

  def postProcess(content: String): String = content
}