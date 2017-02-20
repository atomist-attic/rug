package com.atomist.rug.runtime.rugdsl

class LambdaPredicate[V](
                          val name: String,
                          matcher: V => Boolean,
                          val description: Option[String] = None
                        )
  extends RugDslPredicate[V] {

  override def invoke(ic: FunctionInvocationContext[V]): Boolean = {
    val matched = matcher(ic.target)
    matched
  }
}

object NoOpDslFunction extends RugDslFunction[Object, Object] {

  override def name: String = "NoOp"

  override def invoke(ic: FunctionInvocationContext[Object]) = ic.target

  override def description: Option[String] = Some("No op function. Does nothing")

  override def toString = "NoOp"
}
