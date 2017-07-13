package com.atomist.rug.runtime.js.v8

import java.lang.reflect.Method

import com.atomist.rug.spi.MutableView
import com.eclipsesource.v8._

object RegisterMethodProxy {

  def apply(v8o: V8Object, node: NodeWrapper, obj: AnyRef, method: Method, name: String): Unit = {
    method.getReturnType match {
      case c if c.getName == "void" => v8o.registerJavaMethod(new VoidMethodProxy(node, obj, method), name) // ??
      case _ => v8o.registerJavaMethod(new MethodProxy(node, obj, method), name)
    }
  }
}

/**
  * Call methods on obj from V8
  *
  * @param node
  * @param obj
  * @param method
  */
class MethodProxy(override val node: NodeWrapper,
                  override val obj: AnyRef,
                  override val method: Method)
  extends JavaCallback with V8Proxy {

  override def invoke(receiver: V8Object, parameters: V8Array): AnyRef = {
    invokeImpl(receiver, parameters)
  }
}


class VoidMethodProxy(
                       override val node: NodeWrapper,
                       override val obj: AnyRef,
                       override val method: Method)
  extends JavaVoidCallback with V8Proxy {

  override def invoke(receiver: V8Object, parameters: V8Array): Unit = {
    invokeImpl(receiver, parameters)
  }
}

trait V8Proxy {
  def node: NodeWrapper

  def obj: AnyRef

  def method: Method

  def invokeImpl(receiver: V8Object, parameters: V8Array) : AnyRef = {
    try{
      val args = collectParams(parameters)
      val result = args.length match {
        case o if o > 0 => method.invoke(obj, args: _*)
        case _ => method.invoke(obj)
      }
      obj match {
        case c: MutableView[_] if !Proxy.readOnly(method) =>
          c.commit()
        case _ =>
      }
      Proxy.ifNeccessary(node, result)
    }finally{
      release(parameters)
    }
  }
  /**
    * Collect up parameters for jvm invocation
    *
    * @param parameters
    * @return
    */
  def collectParams(parameters: V8Array): Seq[AnyRef] = {
    val args = new scala.collection.mutable.ListBuffer[AnyRef]
    for (i <- 0 to parameters.length()) {
      val param = parameters.get(i)
      param match {
        case o: V8Object if !o.isUndefined =>
          if (o.contains("__object_reference")) {
            node.get(o) match {
              case Some(x) => args.append(x)
              case None => throw new RuntimeException(s"Could not find a ref while invoking ${method.getName} on $obj")
            }
          } else {
            args.append(new V8JavaScriptObject(node, o))
          }
        case o: V8Object if o.isUndefined =>
        case x => args.append(param)
      }
    }
    args
  }

  def release(parameters: V8Array) {
    for (i <- 0 to parameters.length()) {
      parameters.get(i) match {
        case o: Releasable => o.release()
        case _ =>
      }
    }
  }
}
