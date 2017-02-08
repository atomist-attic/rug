package com.atomist.rug.runtime.js.interop

import com.atomist.rug.kind.service.{Service, ServiceSource}
import com.atomist.util.lang.JavaScriptArray

import scala.collection.JavaConverters._

/**
  * Fronts JavaScript Context object
  */
case class jsContextMatch(root: Object,
                          matches: _root_.java.util.List[jsSafeCommittingProxy],
                          s2: ServiceSource,
                          teamId: String) {

  def services: JavaScriptArray[Service] = new JavaScriptArray[Service](s2.services.asJava)

}
