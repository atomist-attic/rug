package com.atomist.rug.runtime.js.nashorn

import jdk.nashorn.api.scripting.AbstractJSObject

/**
  * Simple JS function that always returns the given value
  */
private[nashorn] class AlwaysReturns(what: AnyRef) extends AbstractJSObject {

  override def isFunction: Boolean = true

  override def call(thiz: scala.Any, args: AnyRef*): AnyRef = what

}
