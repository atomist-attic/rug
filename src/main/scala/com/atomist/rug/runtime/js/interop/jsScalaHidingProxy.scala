package com.atomist.rug.runtime.js.interop

import java.lang.reflect.{Method, Modifier}

import com.atomist.util.lang.JavaScriptArray
import jdk.nashorn.api.scripting.AbstractJSObject
import jdk.nashorn.internal.runtime.ScriptRuntime

import scala.collection.JavaConverters._

import jsScalaHidingProxy.MethodValidator

/**
  * Use this if you want to expose a structure in JavaScript
  * with TypeScript-friendly arrays and null in place of Option.
  * Simply uses reflection.
  * Also, enables suppression of some methods.
  */
class jsScalaHidingProxy private(
                                  target: Any,
                                  methodsToHide: Set[String],
                                  methodValidator: MethodValidator
                                ) extends AbstractJSObject {

  override def getDefaultValue(hint: Class[_]): AnyRef = {
    if (hint == classOf[String]) {
      toString()
    }
    else if (hint == null)
      null
    else
      throw new UnsupportedOperationException("no conversion for " + hint)
  }

  override def getMember(name: String): AnyRef = {
    val r = name match {
      case "toString" =>
        toString
      case _ if methodsToHide.contains(name) =>
        // Make JavaScript folks feel at home.
        ScriptRuntime.UNDEFINED
      case _ if jsSafeCommittingProxy.MagicJavaScriptMethods.contains(name) =>
        super.getMember(name)
      case _ =>
        val m = target.getClass.getMethod(name)
        if (methodValidator(m))
          m.invoke(target)
        else
          ScriptRuntime.UNDEFINED
    }
    //println(s"Raw result for $name=[$r]")
    (r match {
      case seq: Seq[_] => new JavaScriptArray(
        seq.map(new jsScalaHidingProxy(_, methodsToHide, methodValidator))
          .asJava)
      case opt: Option[AnyRef]@unchecked => opt.orNull
      case x => x
    }) match {
      case arr: JavaScriptArray[_] => arr
      case s: String => s
      case i: Integer => i
      case x => jsScalaHidingProxy(x)
    }
  }

  override def toString: String =
    s"${getClass.getSimpleName} wrapping [$target]"
}

object jsScalaHidingProxy {

  type MethodValidator = Method => Boolean

  def apply(target: Any,
            methodsToHide: Set[String] = DefaultMethodsToHide,
            methodValidator: MethodValidator = publicMethodsNotOnObject): jsScalaHidingProxy = {
    val r = target match {
      case null | None => null
      case x => new jsScalaHidingProxy(x, methodsToHide, methodValidator)
    }
    //println(s"Result for $target is $r")
    r
  }

  val DefaultMethodsToHide: Set[String] = Set("getClass")

  def publicMethodsNotOnObject(m: Method): Boolean =
    Modifier.isPublic(m.getModifiers) && m.getDeclaringClass != classOf[Object]
}
