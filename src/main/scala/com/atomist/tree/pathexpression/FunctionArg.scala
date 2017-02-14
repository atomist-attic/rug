package com.atomist.tree.pathexpression

sealed trait FunctionArg

case class StringLiteralFunctionArg(s: String) extends FunctionArg

case class RelativePathFunctionArg(pe: PathExpression) extends FunctionArg