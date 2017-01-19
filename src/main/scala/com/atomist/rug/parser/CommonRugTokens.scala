package com.atomist.rug.parser

/**
  * Common reserved words
  */
trait CommonRugTokens {

  val AndToken = "and"

  val NotToken = "not"

  val OrToken = "or"

  val TrueToken = "true"

  val FalseToken = "false"

  val UsesToken = "uses"

  val LetToken = "let"

  val ReturnToken = "return"

  val DefaultToken = "default"

  val FromToken = "from"

  val CommonReservedWords = Set(
    UsesToken,
    AndToken, OrToken,
    TrueToken, FalseToken,
    FromToken, ReturnToken,
    DefaultToken
  )
}
