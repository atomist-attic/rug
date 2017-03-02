package com.atomist.rug.kind.java

import com.atomist.graph.GraphNode
import com.atomist.rug.kind.core._
import com.atomist.rug.kind.java.JavaSourceType._
import com.atomist.rug.kind.java.JavaTypeType._
import com.atomist.rug.spi._
import com.github.javaparser.ast.body._
import com.github.javaparser.ast.expr.{MarkerAnnotationExpr, NameExpr}

import scala.collection.JavaConverters._

/**
  * Type resolution for a Java type (class or interface)
  */
class JavaTypeType
  extends Type
    with ReflectivelyTypedType {

  override def description = "Java class"

  override def runtimeClass: Class[JavaClassOrInterfaceView] = classOf[JavaClassOrInterfaceView]

  override def findAllIn(context: GraphNode): Option[Seq[MutableView[_]]] = context match {
      case pv: ProjectMutableView =>
        Some(JavaProjectMutableView(pv).javaSourceViews.flatMap(_.childrenNamed(JavaTypeAlias)))
      case fmv: FileArtifactBackedMutableView =>
        Some(
          Seq(new JavaSourceMutableView(fmv.originalBackingObject, JavaProjectMutableView(fmv.parent))).flatMap(_.childrenNamed(JavaTypeAlias))
        )
      case dmv: DirectoryMutableView =>
        val jpmv = JavaProjectMutableView(dmv.parent)
        val filesInDir = dmv.childNodes
        val javaSourceFiles = filesInDir collect {
          case f: FileArtifactBackedMutableView if f.path.endsWith(JavaExtension) =>
            new JavaSourceMutableView(f.originalBackingObject, jpmv)
        }
        val allClasses = javaSourceFiles.flatMap(_.childrenNamed(JavaTypeAlias))
        Some(allClasses)
      case _ => None
    }
}

object JavaTypeType {

  def annotationAddedTo(bd: BodyDeclaration, annotationName: String): Boolean = {
    val annotations = bd.getAnnotations.asScala
    if (!annotations.exists(_.getName.getName == annotationName)) {
      bd.setAnnotations((annotations :+ new MarkerAnnotationExpr(new NameExpr(annotationName))).asJava)
      true
    } else // It's already there
      false
  }

  def annotationRemovedFrom(bd: BodyDeclaration, annotationName: String): Boolean = {
    val annotations = bd.getAnnotations.asScala
    if (annotations.exists(_.getName.getName == annotationName)) {
      bd.setAnnotations(annotations.filterNot(_.getName.getName == annotationName).asJava)
      true
    } else // It's already gone
      false
  }

  val ConstructorAlias: String = "constructor"

  val FieldAlias: String = "field"

  val MethodAlias: String = "method"

  val JavaTypeAlias: String = Typed.typeClassToTypeName(classOf[JavaTypeType])
}
