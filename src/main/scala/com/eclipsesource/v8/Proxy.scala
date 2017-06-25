package com.eclipsesource.v8

import java.lang.reflect.Method

import com.atomist.rug.spi.ExportFunction
import org.springframework.util.ReflectionUtils


/**
  * Create proxies for Java objects
  */
object Proxy {

  def ifNeccessary(runtime: V8, name: String, value: Any, refs: Map[Int, AnyRef]): V8Object = {
    ifNeccessary(runtime, value, refs) match {
      case o: java.lang.Boolean => runtime.add(name, o)
      case d: java.lang.Double => runtime.add(name, d)
      case i: java.lang.Integer => runtime.add(name, i)
      case s: String => runtime.add(name, s)
      case v: V8Value => runtime.add(name, v)
      case x => throw new RuntimeException(s"Could not proxy object of type")
    }
  }

  def ifNeccessary(runtime: V8, value: Any, refs: Map[Int, AnyRef]): AnyRef = value match {
    case r: AnyRef => Proxy(runtime, r, refs)
    case x => x.asInstanceOf[AnyRef] //I think this is boxing?
  }

  /**
    * Create a proxy around JVM obj
    *
    * @param obj
    * @return
    */
  def apply(runtime: V8, obj: AnyRef, refs: Map[Int, AnyRef]): V8Object = {
    val v8pmv = new V8Object(runtime)

    //TODO - is there any other way to track JVM objects as their proxies pass through a JS function?
    val code = obj.hashCode()
    v8pmv.add("__object_reference", code)
    if(!refs.contains(code)){
      throw new RuntimeException(s"Could not find ${code} in ${refs.mkString(",")}")
    }
    obj match {
      case o: Seq[_] =>
        val arr = new V8Array(runtime)
        o.foreach{
          case item: AnyRef => arr.push(Proxy(runtime, item, refs))
        }
        arr
      case _ =>
        obj.getClass.getMethods.foreach {
          case m: Method if ReflectionUtils.isObjectMethod(m) => //don't proxy standard stuff
          case m: Method if exposeAsProperty(m) =>
            m.invoke(obj) match {
              case str: String =>
                //println(s"Adding value ${m.getName}=$str")
                v8pmv.add(m.getName, str)
              case o =>
                //println(s"Proxying property ${m.getName} on ${obj.getClass.getName}")
                v8pmv.add(m.getName, Proxy(runtime, o, Map[Int, AnyRef](o.hashCode() -> o) ++ refs))
            }
          case m: Method if exposeAsFunction(m) =>
            //println(s"Registering method: ${m.getName} on ${obj.getClass.getName}")
            v8pmv.registerJavaMethod(new MethodProxy(runtime, obj,m, refs), m.getName)
          case _ =>
        }
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
  private def exposeAsFunction(m: Method): Boolean = {
    m.getAnnotations.exists {
      case o: ExportFunction => !o.exposeAsProperty()
      case _ => false
    }
  }

  /**
    * Is method m to be exposed as a property?
    * @param m
    * @return
    */
  private def exposeAsProperty(m: Method): Boolean = {
    m.getAnnotations.exists {
      case o: ExportFunction => o.exposeAsProperty()
      case _ => false
    }
  }
}
