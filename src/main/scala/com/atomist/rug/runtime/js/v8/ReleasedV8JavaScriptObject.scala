package com.atomist.rug.runtime.js.v8

import com.atomist.rug.runtime.js.UNDEFINED
import com.eclipsesource.v8.{V8Array, V8Function, V8Object}

/**
  * Assumes obj will be released by the time any of the methods are called
  *
  * Should mean that we could run node remotely at some point!
  *
  * @param node
  * @param obj
  * @param path - list of keys to retrieve the object again
  */
class ReleasedV8JavaScriptObject(node: NodeWrapper, obj: V8Object, path: Seq[String] = Nil)
  extends V8JavaScriptObject(node, obj) {

  private val jsObject = node.getRuntime.get("Object").asInstanceOf[V8Object]
  override val isFunction: Boolean = super.isFunction

  // cache the whole object
  private val allmembers: Map[String, AnyRef] = {
    if(isFunction){
       Map()
    }else{
      val keys = allKeys(obj)
      keys.map(key => (key, obj.get(key))).toMap
    }
  }

  // cache the simple things
  override val isEmpty: Boolean = super.isEmpty
  override val isSeq: Boolean = super.isSeq

  /**
    * Walk prototype chain reading property names
    * @param o
    * @param keys
    * @return
    */
  private def allKeys(o: V8Object, keys: Seq[String] = Nil) : Seq[String] = {
      val props = jsObject.executeJSFunction("getOwnPropertyNames", o) match {
        case keys: V8Array => keys.getStrings(0, keys.length()).toSeq
        case _ => Nil
      }
      jsObject.executeJSFunction("getPrototypeOf", o) match {
        case proto: V8Object if !proto.isUndefined => allKeys(proto, props)
        case _ => keys ++ props
      }
  }

  override def hasMember(name: String): Boolean = allmembers.contains(name)

  override def getMember(name: String): AnyRef =
    allmembers.get(name) match {
      case Some(o: V8Function) => new ReleasedV8JavaScriptObject(node, o, path :+ name)
      case Some(o: AnyRef) => o
      case _ => UNDEFINED
    }

  override def setMember(name: String, value: AnyRef): Unit = super.setMember(name, value)

  override def callMember(name: String, args: AnyRef*): AnyRef = super.callMember(name, args)

  /**
    * Search down graph looking for a function
    * @param p
    * @return
    */
  private def findFunction(o: V8Object, p: Seq[String] = path): Option[V8Function] = {
    o.get(p.head) match {
      case fn: V8Function => Some(fn)
      case nextObj: V8Object => findFunction(nextObj, p.tail)
      case _ => None
    }
  }

  override def call(thisArg: AnyRef, args: AnyRef*): AnyRef = {
    findFunction(node.getRuntime.get(path.head).asInstanceOf[V8Object], path.tail) match {
      case Some(fn: V8Function) => new V8JavaScriptObject(node, fn).call(thisArg, args: _*)
      case _ => throw new IllegalStateException("This is not a function, so don't call me as one")
    }
  }

  override def keys(all: Boolean): Seq[String] = allmembers.keys.toSeq

  override def values(): Seq[AnyRef] = super.values()

  override def toJson(): String = super.toJson()

  /**
    * Return "fields" only
    *
    * @return
    */
  override def extractProperties(): Map[String, AnyRef] = super.extractProperties()

  override def entries(): Map[String, AnyRef] = super.entries()
}
