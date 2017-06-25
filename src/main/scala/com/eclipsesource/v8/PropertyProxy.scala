package com.eclipsesource.v8

import java.lang.reflect.Method

/**
  * V8Value so that we can intercept property access
  * and delegate to a Java method
  */
class PropertyProxy(v8: V8, obj: AnyRef, method: Method)
  extends V8Object(v8) {

  override def executeJSFunction(name: String, parameters: AnyRef*): AnyRef =
    super.executeJSFunction(name, parameters: _*)
}
