package com.atomist.rug.runtime

import com.atomist.param.ParameterValue
import com.atomist.project.ProjectOperationArguments
import com.atomist.rug.RugRuntimeException
import jdk.nashorn.api.scripting.AbstractJSObject

/**
  * Dynamic properties holder that represents the JVM counterpart of a TypeScript class that
  * doesn't exist in Java. Allows additional keys to be set on the proxy.
  */
class BidirectionalParametersProxy(poa: ProjectOperationArguments) extends AbstractJSObject {

  private var _additionalValues: Map[String, Object] = Map()

  /**
    * Original parameters plus additional values written to the proxy from JavaScript
    *
    * @return
    */
  def allMemberValues: Map[String, Object] =
  (poa.parameterValueMap map {
    case (k, v) => (k, v.getValue)
  }) ++ _additionalValues

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

  override def setMember(name: String, value: Object): Unit = {
    _additionalValues = _additionalValues ++ Map(name -> value)
    super.setMember(name, value)
  }
}
