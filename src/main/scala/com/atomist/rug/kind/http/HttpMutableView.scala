package com.atomist.rug.kind.http

import com.atomist.rug.spi._
import org.springframework.http.{HttpEntity, HttpHeaders, HttpMethod, MediaType}
import org.springframework.web.client.RestOperations

import scala.collection.JavaConverters._

class HttpMutableView(httpClient: RestOperations)
  extends ViewSupport[Unit]((), null) {

  override def nodeName: String = "http"

  override def nodeType: String = "http"

  override def childNodeTypes: Set[String] = Set()

  private def exchangeJson(url: String, httpMethod: HttpMethod, data: String): Int = {
    val headers = new HttpHeaders()
    headers.setContentType(MediaType.APPLICATION_JSON)
    headers.setAccept(Seq(MediaType.APPLICATION_JSON).asJava)
    val entity = new HttpEntity[String](data, headers)
    val response = httpClient.exchange(url, httpMethod, entity, classOf[String])
    response.getStatusCode.value()
  }

  //  Currently this only returns http status back.  In future we need to extend this to return response as JSON
  //  that can be navigated using `with`
  //  Ideally the syntax should be instead:
  //
  //  let res = httpPut "http://someplace" "{'foo': 'bar'}"
  //  with res r
  //    r.songs ...
  @ExportFunction(readOnly = true, description = "Execute PUT http request with JSON payload")
  def putJson(@ExportFunctionParameterDescription(name = "url", description = "URL of the http resource") url: String,
              @ExportFunctionParameterDescription(name = "data", description = "JSON payload as a string") data: String): Unit = {
    exchangeJson(url, HttpMethod.PUT, data)
  }

  @ExportFunction(readOnly = true, description = "Execute POST http request with JSON payload")
  def postJson(@ExportFunctionParameterDescription(name = "url", description = "URL of the http resource") url: String,
              @ExportFunctionParameterDescription(name = "data", description = "JSON payload as a string") data: String): Unit = {
    exchangeJson(url, HttpMethod.POST, data)
  }

  override def childrenNames: Seq[String] = Nil

  // TODO not very nice that we need to express children in terms of MutableView, not View, but it's OK for now
  override def children(fieldName: String): Seq[MutableView[_]] = ???
}
