package com.atomist.rug.kind.java.spring

import com.atomist.rug.kind.java.TypeSelection._

import scala.collection.JavaConverters._

/**
  * AddClassAnnotationEditor TypeSelectors fo
  */
object SpringTypeSelectors {

  val SpringBootApplicationClassSelector: TypeSelector =
    coit => coit.getAnnotations.asScala.exists(ann => "SpringBootApplication".equals(ann.getName.getName))

  val SpringMvcControllerClassSelector: TypeSelector =
    coit => coit.getAnnotations.asScala.exists(ann => "RestController".equals(ann.getName.getName))

  val FeignProxyClassSelector: TypeSelector =
    coit => coit.getAnnotations.asScala.exists(ann => "FeignClient".equals(ann.getName.getName))
}
