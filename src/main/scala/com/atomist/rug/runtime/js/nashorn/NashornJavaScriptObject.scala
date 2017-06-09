package com.atomist.rug.runtime.js.nashorn

import com.atomist.rug.runtime.js.JavaScriptObject
import jdk.nashorn.api.scripting.ScriptObjectMirror

/**
  * Nashorn backed JS object
  */
class NashornJavaScriptObject(val som: ScriptObjectMirror)
  extends JavaScriptObject{
  override def hasMember(name: String): Boolean = ???

  override def getMember(name: String): AnyRef = ???

  override def setMember(name: String, value: AnyRef): Unit = ???

  override def callMember(name: String, args: AnyRef*): AnyRef = ???

  override def isEmpty: Boolean = ???

  override def values(): Seq[AnyRef] = ???

  override def isSeq: Boolean = ???

  override def isMap: Boolean = ???

  override def isFunction: Boolean = ???

  override def entries(): Map[String, AnyRef] = ???

  override def call(thisArg: AnyRef, args: AnyRef*): AnyRef = ???

  override def eval(js: String): AnyRef = ???
}
