package com.atomist.rug.kind.java.spring

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration

import scala.collection.JavaConverters._

object SpringTypeSelectors {

  val SpringBootApplicationClassSelector: ClassOrInterfaceDeclaration => Boolean =
    coit => coit.getAnnotations.asScala.exists(ann => "SpringBootApplication".equals(ann.getNameAsString))

  val FeignProxyClassSelector: ClassOrInterfaceDeclaration => Boolean =
    coit => coit.getAnnotations.asScala.exists(ann => "FeignClient".equals(ann.getNameAsString))
}
