package com.atomist.rug.runtime.js.interop

import com.atomist.rug.kind.service.{Service, ServiceSource}
import com.atomist.util.lang.TypeScriptArray

import scala.collection.JavaConverters._

case class ContextMatch(root: Object,
                        matches: _root_.java.util.List[Object],
                        s2: ServiceSource,
                        teamId: String) {

  def services: TypeScriptArray[Service] = new TypeScriptArray[Service](s2.services.asJava)

}
