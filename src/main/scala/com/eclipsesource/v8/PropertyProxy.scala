package com.eclipsesource.v8

import java.lang.reflect.Method

import com.atomist.rug.runtime.js.v8.V8JavaScriptObject
import org.springframework.util.ReflectionUtils

/**
  * Used for Java methods that are exposed as JS properties
  *
  * The underlying method is only called once
  */
class PropertyProxy(node: NodeWrapper, obj: AnyRef, propertyMethod: Method)
  extends V8Object(node.getRuntime) {

  val ret: Class[_] = propertyMethod.getReturnType

  var result: AnyRef = _

  ret.getMethods.foreach(method =>
    (ReflectionUtils.isObjectMethod(method), Proxy.exposeAsFunction(method), Proxy.exposeAsProperty(method)) match {
      case (false, true, false) =>
        if(method.getReturnType == Void.TYPE){
          this.registerJavaMethod(new JavaVoidCallback(){
            override def invoke(receiver: V8Object, parameters: V8Array): Unit = {
              PropertyProxy.this.invoke(method, receiver, parameters)
            }
          }, method.getName)
        }else{
          this.registerJavaMethod(new JavaCallback(){
            override def invoke(receiver: V8Object, parameters: V8Array): AnyRef = {
              PropertyProxy.this.invoke(method, receiver, parameters)
            }
          }, method.getName)
        }
      case (false, false, true) =>
        this.add(method.getName, new PropertyProxy(node, propertyMethod.invoke(obj), method))
      case _ =>
    }
  )

  /**
    * Invoke the named method on the channel
    * @param receiver
    * @param parameters
    * @return
    */
  private def invoke(method: Method, receiver: V8Object, parameters: V8Array): AnyRef = {
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
        case o: V8Object if o.isUndefined => // why do we get this?
        case x => args.append(parameters.get(i))
      }
    }
    //always single threaded!

    if(result == null){
      //so that we only call this lazilly the first time
      result = propertyMethod.invoke(obj) //always no arg arg
    }

    Proxy.ifNeccessary(node, method.invoke(result, args:_*))
  }
}
