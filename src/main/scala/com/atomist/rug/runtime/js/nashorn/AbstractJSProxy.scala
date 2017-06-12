package com.atomist.rug.runtime.js.nashorn

import jdk.nashorn.api.scripting.AbstractJSObject

/**
  * Common behavior for all proxies. Implements default string
  * conversion.
  */
private[nashorn] abstract class AbstractJSProxy extends AbstractJSObject {

  override def getDefaultValue(hint: Class[_]): AnyRef = {
    if (hint == classOf[String]) {
      toString
    }
    else if (hint == null)
      toString
    else
      throw new UnsupportedOperationException("No conversion for " + hint)
  }

}
