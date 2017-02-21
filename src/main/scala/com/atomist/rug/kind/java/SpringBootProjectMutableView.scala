package com.atomist.rug.kind.java

import com.atomist.graph.GraphNode
import com.atomist.rug.kind.core.ProjectMutableView
import com.atomist.rug.kind.java.spring.SpringTypeSelectors
import com.atomist.rug.kind.java.support.JavaAssertions
import com.atomist.rug.runtime.rugdsl.{DefaultEvaluator, Evaluator}
import com.atomist.rug.spi._
import com.atomist.util.lang.JavaHelpers
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration

import scala.collection.JavaConverters._
import scala.util.Try

class SpringBootProjectType(
                             evaluator: Evaluator
                           )
  extends Type(evaluator)
    with ReflectivelyTypedType {

  def this() = this(DefaultEvaluator)

  override def description = "Spring Boot project"

  override def runtimeClass = classOf[SpringBootProjectMutableView]

  override def findAllIn(context: GraphNode): Option[Seq[MutableView[_]]] = context match {
      case jpv: JavaProjectMutableView =>
        val sproj = new SpringBootProjectMutableView(jpv)
        if (sproj.isValid)
          Some(Seq(sproj))
        else Some(Nil)
      case pv: ProjectMutableView if JavaAssertions.isJava(pv.currentBackingObject) =>
        val sproj = new SpringBootProjectMutableView(new JavaProjectMutableView(pv))
        if (sproj.isValid)
          Some(Seq(sproj))
        else Some(Nil)
      case _ =>
        Some(Nil)
    }
}

class SpringBootProjectMutableView(pv: JavaProjectMutableView)
  extends SpringProjectMutableView(pv) {

  def isValid =
    Try { applicationClassFQN }.isSuccess

  @ExportFunction(readOnly = true, description = "The FQN of the Spring Boot Application class")
  def applicationClassFQN: String = {
    val bootAppCompilationUnit: CompilationUnit = compilationUnits.find(cu => cu.getTypes.asScala.collect {
      case coit: ClassOrInterfaceDeclaration if SpringTypeSelectors.SpringBootApplicationClassSelector(coit) => coit
    }.nonEmpty)
      .getOrElse(
        throw new InstantEditorFailureException(s"This is not a Spring Boot project. No application class compilation unit found")
      )
    bootAppCompilationUnit.getPackage.getPackageName + "." + applicationClassSimpleName
  }

  @ExportFunction(readOnly = true, description = "The simple name of the Spring Boot Application class")
  def applicationClassSimpleName: String = {
    val bootAppClass = classes.find(SpringTypeSelectors.SpringBootApplicationClassSelector).getOrElse(
      throw new InstantEditorFailureException(s"This is not a Spring Boot project. No application class found")
    )
    bootAppClass.getName
  }

  @ExportFunction(readOnly = true, description = "The package the Spring Boot Application class is in")
  def applicationClassPackage: String = {
    JavaHelpers.packageFor(applicationClassFQN)
  }

  @ExportFunction(readOnly = false,
    description = "Add the given annotation to the Spring Boot application class")
  def annotateBootApplication(@ExportFunctionParameterDescription(name = "pkg",
    description = "The package of the annotation")
                              pkg: String,
                              @ExportFunctionParameterDescription(name = "annotationName",
                                description = "The annotation to add")
                              annotationName: String): Unit = {
    val ane = new AddClassAnnotationEditor(
      SpringTypeSelectors.SpringBootApplicationClassSelector,
      annotationPackageName = Some(pkg),
      annotationName = annotationName
    )
    updateTo(applyEditor(ane))
  }
}
