package com.atomist.rug.runtime.js.nashorn

import com.atomist.rug.runtime.js.interop.jsScalaHidingProxy
import com.atomist.rug.runtime.js.{JavaScriptObject, UNDEFINED}
import jdk.nashorn.api.scripting.ScriptObjectMirror
import jdk.nashorn.internal.runtime.Undefined

import scala.collection.JavaConverters._

/**
  * Nashorn backed JS object
  */
class NashornJavaScriptObject(val som: ScriptObjectMirror)
  extends JavaScriptObject{

  /**
    * Convert a Nashorn thingy to a generic one
    * @param ref
    * @return
    */
  def convert(ref: AnyRef): AnyRef = {
    ref match {
      case o: ScriptObjectMirror => new NashornJavaScriptObject(o)
      case u: Undefined => UNDEFINED
      case x => x
    }
  }

  override def hasMember(name: String): Boolean = som.hasMember(name)

  override def getMember(name: String): AnyRef = {
    if(som.hasMember(name)){
      convert(som.getMember(name))
    }else{
      UNDEFINED
    }
  }

  override def setMember(name: String, value: AnyRef): Unit = som.setMember(name, value)

  override def callMember(name: String, args: AnyRef*): AnyRef = {
    val wrapped = args.collect {
      case j: jsScalaHidingProxy => j
      case o: Object => new ObjectWrapper(o)
      case x => x
    }
    convert(som.callMember(name, wrapped:_*))
  }

  override def isEmpty: Boolean = som.isEmpty

  override def values(): Seq[AnyRef] = {
    som.values().asScala.map(convert).toSeq
  }

  override def isSeq: Boolean = som.isArray

  override def isFunction: Boolean = som.isFunction

  override def entries(): Map[String, AnyRef] = {
    som.keySet().asScala.flatMap {
      o =>
        try {
          Seq((o, convert(som.get(o))))
        } catch {
          case _: Throwable => Nil
        }
    }.toMap
  }

  override def call(thisArg: AnyRef, args: AnyRef*): AnyRef = {
    val wrapped = args.collect {
      case o: Object => new ObjectWrapper(o)
      case x => x
    }
    convert(som.call(thisArg, wrapped:_*))
  }

  override def eval(js: String): AnyRef = convert(som.eval(js))

  override def getNativeObject: AnyRef = som
}
