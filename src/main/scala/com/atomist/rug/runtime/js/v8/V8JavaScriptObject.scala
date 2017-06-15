package com.atomist.rug.runtime.js.v8

import com.atomist.rug.runtime.js.{JavaScriptObject, UNDEFINED}
import com.eclipsesource.v8.{V8Array, V8Function, V8Object}

/**
  * V8 implementation
  */
class V8JavaScriptObject(obj: AnyRef) extends JavaScriptObject {

  override def getNativeObject: AnyRef = obj

  override def hasMember(name: String): Boolean = {
    obj match {
      case u: V8Object if u.isUndefined => false
      case o: V8Object => o.contains(name)
      case _ => false
    }
  }

  override def getMember(name: String): AnyRef = {
    obj match {
      case u: V8Object if u.isUndefined => UNDEFINED
      case o: V8Object => o.get(name) match {
        case u: V8Object if u.isUndefined => UNDEFINED
        case x: V8Object => new V8JavaScriptObject(x)
        case x => x
      }
      case _ => UNDEFINED
    }
  }

  override def setMember(name: String, value: AnyRef): Unit = ???

  override def callMember(name: String, args: AnyRef*): AnyRef = ???

  /**
    * Create and return a function that can be called later
    * which closes over the name
    *
    * @param name
    * @return
    */
  override def createMemberFunction(name: String): AnyRef = ???

  override def call(thisArg: AnyRef, args: AnyRef*): AnyRef = ???

  override def isEmpty: Boolean = obj match {
    case v: V8Array => v.length() == 0
    case x: V8Object => x.getKeys.length == 0
    case _ => false
  }

  override def values(): Seq[AnyRef] = obj match {
    case x: V8Object => x.getKeys.map(p => {
      x.get(p) match {
        case o: V8Object => new V8JavaScriptObject(o)
        case x => x
      }
    })
    case _ => Nil
  }

  override def isSeq: Boolean = {
    obj match {
      case u: V8Object if u.isUndefined => false
      case _: V8Array => true
      case _ => false
    }
  }

  override def isFunction: Boolean = obj match {
    case u: V8Function => true
    case _ => false
  }

  override def eval(js: String): AnyRef = ???

  override def entries(): Map[String, AnyRef] = ???
}
