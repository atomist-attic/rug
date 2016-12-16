package com.atomist.rug.parser

/**
  * Common reserved words
  */
trait CommonRugTokens {

  val AndToken = "and"

  val NotToken = "not"

  val OrToken = "or"

  val TrueToken = "true"

  protected val trueToken = TrueToken.r

  val FalseToken = "false"

  protected val falseToken = FalseToken.r

  val UsesToken = "uses"

  protected val usesToken = UsesToken.r

  val LetToken = "let"

  val FromToken = "from"

  val fromToken = FromToken.r

  val ReturnToken = "return"

  val returnToken = ReturnToken.r

  protected val letToken = LetToken.r

  val DefaultToken = "default"

  val defaultToken = DefaultToken.r

  val CommonReservedWords = Set(
    UsesToken,
    AndToken, OrToken,
    TrueToken, FalseToken,
    FromToken, ReturnToken,
    DefaultToken
  )
}
