package com.atomist.rug.runtime.js.v8

import com.atomist.rug.runtime.js.{JavaScriptObject, UNDEFINED}
import com.eclipsesource.v8.{V8Array, V8Function, V8Object, V8Value}

/**
  * V8 implementation
  */
class V8JavaScriptObject(node: NodeWrapper, obj: V8Object)
  extends JavaScriptObject {

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
        case x: V8Object => new V8JavaScriptObject(node, x)
        case x => x
      }
      case _ => UNDEFINED
    }
  }

  override def setMember(name: String, value: AnyRef): Unit = {
    Proxy.addIfNeccessary(obj, node, name, value)
  }

  override def callMember(name: String, args: AnyRef*): AnyRef = {

    val proxied = args.map(a => Proxy.ifNeccessary(node, a))
    obj.asInstanceOf[V8Object].executeJSFunction(name, proxied:_*) match {
      case u: V8Object if u.isUndefined => UNDEFINED
      case o: V8Object => new V8JavaScriptObject(node, o)
      case x => x
    }
  }

  override def call(thisArg: AnyRef, args: AnyRef*): AnyRef = {
    Proxy.withMemoryManagement(node, {
      obj match {
        case o: V8Function =>
          val params = new V8Array(o.getRuntime)
          args.foreach(a => Proxy.ifNeccessary(node, a) match {
            case o: java.lang.Boolean => params.push(o)
            case d: java.lang.Double => params.push(d)
            case i: java.lang.Integer => params.push(i)
            case s: String => params.push(s)
            case v: V8Value => params.push(v)
            case x => throw new RuntimeException(s"Could not proxy object $x")
          })
          o.call(null, params) match {
            case x: V8Object if !x.isUndefined => node.get(x) match {
              case Some(jvmObj) => jvmObj
              case _ => new V8JavaScriptObject(node, x)
            }
            case _: V8Object => UNDEFINED
            case o => o
          }
      }
    })
  }

  override def isEmpty: Boolean = obj match {
    case v: V8Array => v.length() == 0
    case x: V8Object => keys().isEmpty
    case _ => false
  }

  override def values(): Seq[AnyRef] = {
    keys().map(p => {
      obj.get(p) match {
        case o: V8Object => new V8JavaScriptObject(node, o)
        case x => x
      }
    })
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

  override def eval(js: String): AnyRef = {
    throw new UnsupportedOperationException
  }

  override def entries(): Map[String, AnyRef] = {
    keys().map { key =>
      obj.get(key) match {
        case o: V8Object => (key, new V8JavaScriptObject(node, o))
        case eh => (key, eh)
      }
    }.toMap
  }

  /**
    * Because V8 doesn't return a prototypes properties
    * when calling getKeys
    *
    * @return
    */
  private def keys(): Seq[String] = {
    if(obj.isUndefined){
      Nil
    }else{
      Proxy.withMemoryManagement(node, {
        val objKeys = obj.getKeys
        val json = node.getRuntime.get("Object").asInstanceOf[V8Object]
        val protoKeys = json.executeJSFunction("getPrototypeOf", obj) match {
          case p: V8Object if !p.isUndefined =>
            p.getKeys.filter(protoKey => {
              json.executeJSFunction("getOwnPropertyDescriptor", p, protoKey) match {
                case o: V8Object if  !o.isUndefined  && o.contains("get") => false
                case _ => true
              }}).toSeq
          case _ => Nil
        }
        objKeys ++ protoKeys
      })
    }
  }

  override def toJson(): String = {
    val json = node.getRuntime.get("JSON").asInstanceOf[V8Object]
    json.executeJSFunction("stringify", obj).asInstanceOf[String]
  }

  override def keys(all: Boolean): Iterable[String] = {
    if(all){
      keys()
    }else{
      obj.getKeys
    }
  }
}
