package com.eclipsesource.v8

import java.lang.reflect.Method

import com.atomist.rug.runtime.js.v8.V8JavaScriptObject


/**
  * Call methods on obj from V8
  * @param runtime
  * @param obj
  * @param method
  */
class MethodProxy(runtime: V8, obj: AnyRef, method: Method, refs: Map[Int, AnyRef]) extends JavaCallback {

  override def invoke(receiver: V8Object, parameters: V8Array): AnyRef = {
    val args = new scala.collection.mutable.ListBuffer[AnyRef]
    for(i <- 0 to parameters.length()){
      parameters.get(i) match {
        case o: V8Object if !o.isUndefined =>
          if(o.contains("__object_reference")){
            refs.get(o.get("__object_reference").asInstanceOf[Int]) match {
              case Some(x) => args.append(x)
              case x => args.append(x)
            }

          }else{
            args.append(new V8JavaScriptObject(o))
          }
        case o: V8Object if o.isUndefined =>
        case x => args.append(parameters.get(i))
      }
    }
    Proxy.ifNeccessary(runtime, method.invoke(obj, args:_*), refs)
  }
}
