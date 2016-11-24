package com.atomist.rug.kind.java.spring

import com.atomist.rug.kind.java.TypeSelection._

import scala.collection.JavaConversions._

/**
  * AddClassAnnotationEditor TypeSelectors fo
  */
object SpringTypeSelectors {

  val SpringBootApplicationClassSelector: TypeSelector =
    coit => coit.getAnnotations.exists(ann => "SpringBootApplication".equals(ann.getName.getName))

  val SpringMvcControllerClassSelector: TypeSelector =
    coit => coit.getAnnotations.exists(ann => "RestController".equals(ann.getName.getName))

  val FeignProxyClassSelector: TypeSelector =
    coit => coit.getAnnotations.exists(ann => "FeignClient".equals(ann.getName.getName))
}
