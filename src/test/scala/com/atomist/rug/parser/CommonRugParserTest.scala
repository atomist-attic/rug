package com.atomist.rug.parser

object CommonRugParserTest {

  val EqualsLiteralStringInPredicate: String =
    s"""
       |@description '100% JavaScript free'
       |editor Triplet
       |
       |with File f
       | when name = "thing"
       |
       |do
       | append "foobar"
      """.stripMargin

  val EqualsLetStringInPredicate: String =
    s"""
       |@description '100% JavaScript free'
       |editor Triplet
       |
       |let checkFor = "thing"
       |
       |with File f
       | when name = checkFor
       |
       |do
       | append "foobar"
      """.stripMargin

  val EqualsLiteralStringInPredicatesWithParam: String =
    s"""
       |@description '100% JavaScript free'
       |editor Triplet
       |
       |param what: ^.*$$
       |
       |with File f
       | when name = "thing"
       |
       |do
       | append what
      """.stripMargin

  val InvokeOtherOperationWithSingleParameter: String =
    s"""
       |editor Triplet
       |
       |@tag "java"
       |@tag "spring"
       |param javaThing: @java_package
       |
       |Foobar
      """.stripMargin

  val WellKnownRegexInParameter: String =
    s"""
       |editor Triplet
       |
       |param javaThing: @java_identifier
       |
       |Foobar
      """.stripMargin

  val EqualsJavaScriptBlockInPredicate: String =
    s"""
       |@description '100% JavaScript free'
       |editor Triplet
       |
       |with File f
       | when isJava = { "thing" }
       |
       |do
       | append "foobar"
      """.stripMargin

}
