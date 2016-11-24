package com.atomist.rug.runtime.lang

trait Invocable {

  def invokeFunction(context: Object, argsToUse: Map[String, Object]): Object
}
