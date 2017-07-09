package com.eclipsesource.v8

import java.lang.reflect.{Field, Method}

import com.atomist.rug.runtime.js.interop.ExposeAsFunction
import com.atomist.rug.runtime.js.v8.V8JavaScriptObject
import com.atomist.rug.spi.ExportFunction
import org.apache.commons.lang3.ClassUtils
import org.springframework.core.annotation.AnnotationUtils
import org.springframework.util.ReflectionUtils

import scala.collection.JavaConverters._

/**
  * Create proxies for Java objects
  */
object Proxy {

  def pushIfNeccessary(v8Array: V8Array, node: NodeWrapper, value: Any): V8Object = {
    ifNeccessary(node, value) match {
      case o: java.lang.Boolean => v8Array.push(o)
      case d: java.lang.Double => v8Array.push(d)
      case i: java.lang.Integer => v8Array.push(i)
      case s: String => v8Array.push( s)
      case v: V8Value => v8Array.push(v)
      case x => throw new RuntimeException(s"Could not push object: $x")
    }
  }
  def addIfNeccessary(v8Object: V8Object, node: NodeWrapper, name: String, value: Any): V8Object = {
    ifNeccessary(node, value) match {
      case o: java.lang.Boolean => v8Object.add(name, o)
      case d: java.lang.Double => v8Object.add(name, d)
      case i: java.lang.Integer => v8Object.add(name, i)
      case s: String => v8Object.add(name, s)
      case v: V8Value => v8Object.add(name, v)
      case x => throw new RuntimeException(s"Could not add object: $x")
    }
  }

  def addIfNeccessary(node: NodeWrapper, name: String, value: Any): V8Object = {
    val runtime = node.getRuntime
    ifNeccessary(node, value) match {
      case o: java.lang.Boolean => runtime.add(name, o)
      case d: java.lang.Double => runtime.add(name, d)
      case i: java.lang.Integer => runtime.add(name, i)
      case s: String => runtime.add(name, s)
      case v: V8Value => runtime.add(name, v)
      case x => throw new RuntimeException(s"Could not proxy object of type")
    }
  }

  def ifNeccessary(node: NodeWrapper, value: Any): AnyRef = value match {
    case o: AnyRef if ClassUtils.isPrimitiveWrapper(o.getClass) =>
      o match {
        case l: java.lang.Long => l.intValue().asInstanceOf[AnyRef]
        case _ => o
      }
    case s: String => s
    case v: V8Value => v
    case o: V8JavaScriptObject => o.getNativeObject
    case Some(r: AnyRef) => Proxy(node, r)
    case r: AnyRef => Proxy(node, r)
    case x => x.asInstanceOf[AnyRef] //I think this is boxing?
  }

  // because for some reason we can't use classOf when matching!
  // AND they have to be upper case!!!!
  private val JList = classOf[java.util.List[_]]
  private val JString = classOf[String]
  private val JLong = classOf[Long]
  private val JDouble = classOf[Double]
  private val JInt = classOf[Int]
  private val JBoolean = classOf[Boolean]

  /**
    * Create a proxy around JVM obj
    *
    * @param obj
    * @return
    */
  private def apply(node: NodeWrapper, obj: AnyRef): V8Object = {

    if(ClassUtils.isPrimitiveWrapper(obj.getClass) || obj.isInstanceOf[String]){
      throw new RuntimeException(s"Should not try to proxy primitive wrappers or strings: $obj")
    }

    val v8pmv = new V8Object(node.getRuntime)

    node.put(v8pmv, obj)

    obj match {
      case o: Seq[_] =>
        val arr = new V8Array(node.getRuntime)
        o.foreach{
          case item: AnyRef =>
            Proxy.pushIfNeccessary(arr, node, item)
        }
        arr
      case l: java.util.List[_] =>
        val arr = new V8Array(node.getRuntime)
        l.asScala.foreach{
          case item: AnyRef => Proxy.pushIfNeccessary(arr, node, item)
        }
        arr
      case l: Set[_] =>
        val arr = new V8Array(node.getRuntime)
        l.foreach{
          case item: AnyRef => Proxy.pushIfNeccessary(arr, node, item)
        }
        arr
      case _ =>
        obj.getClass.getMethods.foreach {
          case m: Method if ReflectionUtils.isObjectMethod(m) => //don't proxy standard stuff
          case m: Method if exposeAsProperty(m) =>
            m.getReturnType match {
              case JString => v8pmv.add(m.getName, m.invoke(obj).asInstanceOf[String])
              case JLong => v8pmv.add(m.getName, m.invoke(obj).asInstanceOf[Long])
              case JInt => v8pmv.add(m.getName, m.invoke(obj).asInstanceOf[Int])
              case JDouble => v8pmv.add(m.getName, m.invoke(obj).asInstanceOf[Double])
              case JBoolean => v8pmv.add(m.getName, m.invoke(obj).asInstanceOf[Boolean])
              case _ =>
                val callback = new V8Object(node.getRuntime)
                callback.registerJavaMethod(new MethodProxy(node, obj,m), "get")
                callback.add("configurable", true)
                val theObject = node.getRuntime.get("Object").asInstanceOf[V8Object]
                theObject.executeJSFunction("defineProperty", v8pmv, m.getName, callback)
            }
          case m: Method if exposeAsFunction(m) =>
            v8pmv.registerJavaMethod(new MethodProxy(node, obj,m), m.getName)
          case _ =>
        }
        /**
          * Use getters for fields to so that we don't need to proxy recursively
          */
        obj.getClass.getFields.foreach(f => {
          f.getType match {
            case JString => v8pmv.add(f.getName, f.get(obj).asInstanceOf[String])
            case JLong => v8pmv.add(f.getName, f.get(obj).asInstanceOf[Long])
            case JInt => v8pmv.add(f.getName, f.get(obj).asInstanceOf[Int])
            case JDouble => v8pmv.add(f.getName, f.get(obj).asInstanceOf[Double])
            case JBoolean => v8pmv.add(f.getName, f.get(obj).asInstanceOf[Boolean])
            case _ =>
              val callback = new V8Object(node.getRuntime)
              callback.registerJavaMethod(new JavaCallback {
                override def invoke(receiver: V8Object, parameters: V8Array): AnyRef = {
                  Proxy.ifNeccessary(node, f.get(obj))
                }
              }, "get")
              callback.add("configurable", true)
              val theObject = node.getRuntime.get("Object").asInstanceOf[V8Object]
              theObject.executeJSFunction("defineProperty", v8pmv, f.getName, callback)
          }
        })
        v8pmv
    }
  }

  private def isProxyable(o: Any): Boolean = {
    o match {
      case o: String => false
      case o: Integer => false
      case x => true
    }
  }

  /**
    * Is method m to be exposed as a function?
    * @param m
    * @return
    */
  def exposeAsFunction(m: Method): Boolean = {
    (AnnotationUtils.findAnnotation(m, classOf[ExportFunction]), AnnotationUtils.findAnnotation(m, classOf[ExposeAsFunction])) match {
      case (o: ExportFunction, null) => !o.exposeAsProperty()
      case (null, o: ExposeAsFunction) => true
      case _ => false
    }
  }

  /**
    * Is method m to be exposed as a property?
    * @param m
    * @return
    */
  def exposeAsProperty(m: Method): Boolean = {
    AnnotationUtils.findAnnotation(m, classOf[ExportFunction]) match {
      case o: ExportFunction => o.exposeAsProperty()
      case _ => false
    }
  }
}
