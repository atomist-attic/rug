package com.eclipsesource.v8

import java.lang.reflect.Method

import com.atomist.rug.runtime.js.v8.V8JavaScriptObject
import com.atomist.rug.spi.MutableView


/**
  * Call methods on obj from V8
  * @param node
  * @param obj
  * @param method
  */
class MethodProxy(node: NodeWrapper, obj: AnyRef, method: Method) extends JavaCallback {

  override def invoke(receiver: V8Object, parameters: V8Array): AnyRef = {
    val args = new scala.collection.mutable.ListBuffer[AnyRef]
    for(i <- 0 to parameters.length()){
      parameters.get(i) match {
        case o: V8Object if !o.isUndefined =>
          if(o.contains("__object_reference")){
            node.get(o) match {
              case Some(x) => args.append(x)
              case None => throw new RuntimeException(s"Could not find a ref while invoking ${method.getName} on $obj")
            }
          }else{
            args.append(new V8JavaScriptObject(node, o))
          }
        case o: V8Object if o.isUndefined =>
        case x => args.append(parameters.get(i))
      }
    }

    val result = method.invoke(obj, args:_*)
    obj match {
      case c: MutableView[_] if !Proxy.readOnly(method) =>
        c.commit()
      case _ =>
    }
    Proxy.ifNeccessary(node, result)
  }
}
