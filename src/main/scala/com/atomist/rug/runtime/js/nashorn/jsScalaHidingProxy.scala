package com.atomist.rug.runtime.js.nashorn

import java.lang.reflect.{InvocationTargetException, Method, Modifier}

import com.atomist.graph.GraphNode
import com.atomist.rug.runtime.Rug
import com.atomist.rug.runtime.js.interop._
import com.atomist.rug.runtime.js.nashorn.jsScalaHidingProxy.MethodValidator
import com.atomist.rug.runtime.js.{JavaScriptCommandHandler, JavaScriptObject}
import jdk.nashorn.api.scripting.{AbstractJSObject, ScriptObjectMirror}
import jdk.nashorn.internal.runtime.ScriptRuntime
import org.apache.commons.lang3.ClassUtils
import org.springframework.util.ReflectionUtils

import scala.collection.JavaConverters._
import scala.util.control.NonFatal

/**
  * Use this if you want to expose a structure in JavaScript
  * with TypeScript-friendly arrays and null in place of Option.
  * Simply uses reflection.
  * Also, enables suppression of some methods.
  * Exposes no-arg methods as properties but allows invocation with parameters.
  * If you wish to expose a no-arg method as a function (because it has
  * side effects etc) use
  *
  * @see ExposeAsFunction
  */
class jsScalaHidingProxy private(
                                  val target: Any,
                                  methodsToHide: Set[String],
                                  methodValidator: MethodValidator
                                ) extends AbstractJSProxy {

  if(target.isInstanceOf[String])
    throw new RuntimeException("This can't be a string!")//TODO remove

  if(target.isInstanceOf[Boolean])
    throw new RuntimeException("This can't be a boolean!")//TODO remove

  if(target.isInstanceOf[jsSafeCommittingProxy])
    throw new RuntimeException("Can't be a proxy")

  if(target.isInstanceOf[JavaScriptCommandHandler]){
    throw new RuntimeException("Can't be a handler")
  }
  override def getMember(name: String): AnyRef = {
    val r = name match {
      case _ if methodsToHide.contains(name) =>
        // Make JavaScript folks feel at home.
        ScriptRuntime.UNDEFINED
      case _ if jsSafeCommittingProxy.MagicJavaScriptMethods.contains(name) =>
        super.getMember(name)
      case _ =>
        val theTarget = target match {
          case Some(x) => x
          case x => x
        }
        val methods = ReflectionUtils.getAllDeclaredMethods(theTarget.getClass).filter(_.getName == name)
        try {
          methods.headOption match {
            case Some(m) if methodValidator(m) =>
              if (m.getParameterCount == 0 && !m.isAnnotationPresent(classOf[ExposeAsFunction]))
                m.invoke(theTarget) match {
                  case s: ScriptObjectBackedTreeNode => jsScalaHidingProxy(s)
                  case n: GraphNode => n
                  case o: JavaScriptObject => o
                  case s: String => s
                  case m: ScriptObjectMirror => m
                  case p: Object if ClassUtils.isPrimitiveWrapper(p.getClass) => p
                  case a: NashornJavaScriptArray[_] => a
                  case x: Seq[_] => x
                  case o: AnyRef => jsScalaHidingProxy(o)
                  case x => x
                }
              else {
                new FunctionProxyToReflectiveInvocation(methods)
              }
            case _ =>
              ScriptRuntime.UNDEFINED
          }
        }
        catch {
          case _: NoSuchMethodException =>
            ScriptRuntime.UNDEFINED
        }
    }

    (r match {
      case seq: Seq[_] => new NashornJavaScriptArray(
        seq.map(new jsScalaHidingProxy(_, methodsToHide, methodValidator))
          .asJava)
      case opt: Option[AnyRef]@unchecked => opt.orNull
      case x => x
    }) match {
      case null => null
      case ScriptRuntime.UNDEFINED => ScriptRuntime.UNDEFINED
      case arr: NashornJavaScriptArray[_] => arr
      case s: String => s
      case i: Integer => i
      case fun: FunctionProxyToReflectiveInvocation => fun
      case js: ScriptObjectMirror => js
      case njo: NashornJavaScriptObject => njo.som
      case o if ClassUtils.isPrimitiveOrWrapper(o.getClass) => o
      case safe: jsSafeCommittingProxy => safe
      case x => jsScalaHidingProxy(x)
    }
  }

  private class FunctionProxyToReflectiveInvocation(methods: Seq[Method])
    extends AbstractJSObject {

    override def isFunction: Boolean = true

    override def call(thiz: scala.Any, args: AnyRef*): AnyRef = {
      val m = methods.find(_.getParameterCount == args.size).getOrElse(throw new RuntimeException(s"Could not find method '${methods.head.getName}' with ${args.size}" +
        s" parameters"))
      val fixed = args.collect {
        case o: ScriptObjectMirror => new NashornJavaScriptObject(o)
        case x => x
      }
      try {
        val res = (if(fixed.nonEmpty) m.invoke(target, fixed: _*) else m.invoke(target)) match {
          case s: ScriptObjectBackedTreeNode => jsScalaHidingProxy(s)
          case n: GraphNode => n
          case pxe: jsPathExpressionEngine => jsScalaHidingProxy(pxe)
          case o: Object
            if !o.isInstanceOf[String] &&
              !o.isInstanceOf[JavaScriptObject] &&
              !o.isInstanceOf[ScriptObjectMirror] &&
              !ClassUtils.isPrimitiveWrapper(o.getClass) &&
              !o.isInstanceOf[NashornJavaScriptArray[_]] &&
              !o.isInstanceOf[Rug]
              => jsScalaHidingProxy(o)
          case o: NashornJavaScriptObject => o.som
          case null => null;
          case y: AnyRef => y
        }
        res
      }
      catch {
        case i: IllegalArgumentException =>
          throw new RuntimeException(s"Error invoking ${m.getName} on $target with ${args.length} parameters: [${fixed.mkString(",")}]", i)
        case t: InvocationTargetException =>
          throw t.getTargetException
        case NonFatal(t) => throw t
      }
    }
  }

  override def equals(that: Any): Boolean = that match {
    case jsp: jsScalaHidingProxy =>
      this.target == jsp.target
    case _ => false
  }

  override def toString: String =
    s"${getClass.getSimpleName} wrapping [$target]"
}

object jsScalaHidingProxy {

  type MethodValidator = Method => Boolean

  def apply(target: Any,
            methodsToHide: Set[String] = DefaultMethodsToHide,
            methodValidator: MethodValidator = publicMethodsNotOnObject,
            returnNotToProxy: Any => Boolean = _ => false): jsScalaHidingProxy = {
    val r = target match {
      case null | None => null
      case shp: jsScalaHidingProxy =>
        // Don't double proxy
        shp
      case x =>
        new jsScalaHidingProxy(x, methodsToHide, methodValidator)
    }
    r
  }

  val DefaultMethodsToHide: Set[String] = Set("getClass")

  def publicMethodsNotOnObject(m: Method): Boolean =
    Modifier.isPublic(m.getModifiers) && m.getDeclaringClass != classOf[Object]

}
