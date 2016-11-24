package com.atomist.rug.runtime

class LambdaPredicate[V](
                          val name: String,
                          matcher: V => Boolean,
                          val description: Option[String] = None
                        )
  extends RugPredicate[V] {

  override def invoke(ic: FunctionInvocationContext[V]): Boolean = {
    val matched = matcher(ic.target)
    matched
  }
}

object NoOpFunction extends RugFunction[Object, Object] {

  override def name: String = "NoOp"

  override def invoke(ic: FunctionInvocationContext[Object]) = ic.target

  override def description: Option[String] = Some("No op function. Does nothing")

  override def toString = "NoOp"
}
