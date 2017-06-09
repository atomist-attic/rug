package com.atomist.rug.runtime.plans

import com.atomist.rug.runtime.js.{JavaScriptEngineContext, JavaScriptObject}
import com.atomist.rug.spi.Handlers.Response
import com.atomist.util.JsonUtils


/**
  * Convert a string or byte array to an JS Object.
  */
class JsonResponseConverter(jsc: JavaScriptEngineContext, handler: JavaScriptObject) extends ResponseConverter {

  override def convert(response: Response): Response = {
    val body = response.body match {
      case Some(bytes: Array[Byte]) => new String(bytes)
      case Some(str: String) => str
      case Some(o) => JsonUtils.toJson(o)
      case agg => throw new RuntimeException(s"Could not recognize body type for coercion: $agg")
    }
    Response(response.status, response.msg, response.code, Some(jsc.parseJson(body)))
  }
}
