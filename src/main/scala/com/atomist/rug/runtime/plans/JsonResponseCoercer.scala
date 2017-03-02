package com.atomist.rug.runtime.plans

import javax.script.{ScriptContext, SimpleBindings}

import com.atomist.rug.runtime.js.{JavaScriptContext, JsonSerializer}
import com.atomist.rug.spi.Handlers.Response
import jdk.nashorn.api.scripting.ScriptObjectMirror
import scala.collection.JavaConverters._

/**
  * Convert a string or byte array to an JS Object
  */
class JsonResponseCoercer(jsc: JavaScriptContext, handler: ScriptObjectMirror) extends ResponseCoercer {

  override def coerce(response: Response): Response = {
    val body = response.body match {
      case Some(bytes: Array[Byte]) => new String(bytes)
      case Some(str: String) => str
      case Some(o) => JsonSerializer.toJson(o)
      case agg => throw new RuntimeException(s"Could not recognize body type for coercion: $agg")
    }
    Response(response.status, response.msg, response.code, Some(toJSObject(body)))
  }

  private def toJSObject(body: String): AnyRef = {

    val bindings = new SimpleBindings()
    bindings.put("__coercion",body)

    //TODO - why do we need this?
    jsc.engine.getContext.getBindings(ScriptContext.ENGINE_SCOPE).asScala.foreach{
      case (k: String, v: AnyRef) => bindings.put(k,v)
    }
    jsc.engine.eval(s"""JSON.parse(__coercion);""", bindings)
  }
}
