package com.atomist.rug.runtime.js.nashorn

import com.atomist.graph.GraphNode
import com.atomist.rug.kind.DefaultTypeRegistry
import com.atomist.rug.runtime.js.{JavaScriptObject, UNDEFINED}
import com.atomist.util.JsonUtils
import jdk.nashorn.api.scripting.{AbstractJSObject, ScriptObjectMirror}
import jdk.nashorn.internal.runtime.Undefined
import org.apache.commons.lang3.ClassUtils

import scala.collection.JavaConverters._

/**
  * Nashorn backed JS object
  */
private[nashorn] class NashornJavaScriptObject(val som: ScriptObjectMirror)
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
      case j: jsSafeCommittingProxy => j
      case o: Object => jsScalaHidingProxy(o)
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
    val wrapped = args.map{
      case o: jsSafeCommittingProxy => o
      case o: String => o
      case o: GraphNode => new jsSafeCommittingProxy(o, DefaultTypeRegistry)
      case o: Object if ClassUtils.isPrimitiveWrapper(o.getClass) => o
      case a => jsScalaHidingProxy(a)
    }
    convert(som.call(thisArg, wrapped:_*))
  }

  override def eval(js: String): AnyRef = convert(som.eval(js))

  override def getNativeObject: AnyRef = som

  /**
    * Need this cos people are using this in maps etc
    * @return
    */
  override def hashCode(): Int = som.hashCode()

  /**
    * Need this cos people are using this in maps etc
    * @return
    */
  override def equals(obj: scala.Any): Boolean = {
    obj match {
      case n: NashornJavaScriptObject => n.som.equals(som)
      case _ => false
    }
  }

  /**
    * Create and return a function that can be called later
    * which closes over the name
    *
    * @param name
    * @return
    */
  override def createMemberFunction(name: String): AnyRef = {
    new InvokingFunctionProxy(name)
  }

  private class InvokingFunctionProxy(fname: String)
    extends AbstractJSObject {
    override def isFunction: Boolean = true
    override def call(thiz: scala.Any, args: AnyRef*): AnyRef = {
      som.callMember(fname, args:_*)
    }
  }

  override def toJson(): String = JsonUtils.toJsonStr(som)
}
