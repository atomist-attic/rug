package com.atomist.rug.runtime

import com.atomist.param.ParameterValue
import com.atomist.project.ProjectOperationArguments
import com.atomist.rug.RugRuntimeException
import jdk.nashorn.api.scripting.AbstractJSObject

/**
  * Dynamic properties holder that represents the JVM counterpart of a TypeScript class that
  * doesn't exist in Java.
  */
private[runtime] class ParametersProxy(poa: ProjectOperationArguments) extends AbstractJSObject {

  override def getMember(name: String): AnyRef = {
    val resolved: ParameterValue = poa.parameterValueMap.getOrElse(
      name,
      throw new RugRuntimeException(null, s"Cannot resolve parameter [$name]"))
    //println(s"Call to getMember with [$name]")

    // The below is what you use for a function
    //    new AbstractJSObject() {
    //
    //      override def isFunction: Boolean = true
    //
    //      override def call(thiz: scala.Any, args: AnyRef*): AnyRef = {
    //        resolved
    //      }
    //    }

    // This works for a method
    resolved.getValue
    // TODO fall back to the value in the field?
  }
}
