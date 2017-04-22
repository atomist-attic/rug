package com.atomist.rug.kind.java

import com.atomist.graph.GraphNode
import com.atomist.rug.kind.core.{FileArtifactBackedMutableView, ProjectMutableView}
import com.atomist.rug.spi._
import com.typesafe.scalalogging.LazyLogging

/**
  * Represents a Java source file.
  * Note: This is the first generation, verb-oriented Java type.
  * It supports path expressions into itself but no deeper. Use
  * the ANTLR-based JavaFileType if you wish to use path expressions throughout
  * the Java AST.
  *
  * This type may be deleted in a future version of Rug, if the JavaFileType
  * and TypeScript helpers prove capable of replacing all its functionality
  * in a convenient manner.
  */
class JavaSourceType
  extends Type
    with ReflectivelyTypedType
    with LazyLogging {

  override def description = "Java source file"

  override def runtimeClass: Class[JavaSourceMutableView] = classOf[JavaSourceMutableView]

  override def findAllIn(context: GraphNode): Option[Seq[MutableView[_]]] = context match {
      case pv: ProjectMutableView =>
        Some(JavaProjectMutableView(pv).javaSourceViews)
      case fmv: FileArtifactBackedMutableView if fmv.path.endsWith(JavaSourceType.JavaExtension)=>
        val jpv = JavaProjectMutableView(fmv.parent)
        Some(Seq(new JavaSourceMutableView(fmv.currentBackingObject, jpv)))
      case _ => None
    }
}

object JavaSourceType {

  val JavaExtension = ".java"

  val FieldAlias = "JavaField"
}
