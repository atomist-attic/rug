package com.atomist.rug.kind.java

import com.atomist.project.ProjectOperationArguments
import com.atomist.rug.kind.core._
import com.atomist.rug.kind.dynamic.ContextlessViewFinder
import com.atomist.rug.kind.java.JavaTypeType._
import com.atomist.rug.parser.Selected
import com.atomist.rug.runtime.rugdsl.{DefaultEvaluator, Evaluator}
import com.atomist.rug.spi._
import com.atomist.source.ArtifactSource
import com.github.javaparser.ast.body._
import com.github.javaparser.ast.expr.{MarkerAnnotationExpr, NameExpr}

import scala.collection.JavaConverters._

class JavaTypeType(evaluator: Evaluator)
  extends Type(evaluator)
    with ContextlessViewFinder
    with ReflectivelyTypedType {

  def this() = this(DefaultEvaluator)

  override val resolvesFromNodeTypes: Set[String] =
    Typed.typeClassesToTypeNames(classOf[ProjectType], classOf[FileType], classOf[JavaSourceType])

  override def description = "Java class"

  override def viewManifest: Manifest[JavaClassOrInterfaceView] = manifest[JavaClassOrInterfaceView]

  override protected def findAllIn(rugAs: ArtifactSource, selected: Selected, context: MutableView[_],
                                   poa: ProjectOperationArguments, identifierMap: Map[String, Object]): Option[Seq[MutableView[_]]] =
    context match {
      case pv: ProjectMutableView =>
        Some(JavaProjectMutableView(pv).javaSourceViews.flatMap(_.children(JavaTypeAlias)))
      case fmv: FileArtifactBackedMutableView =>
        Some(
          Seq(new JavaSourceMutableView(fmv.originalBackingObject, JavaProjectMutableView(fmv.parent))).flatMap(s => s.children(JavaTypeAlias))
        )
      case dmv: DirectoryMutableView =>
        val jpmv = JavaProjectMutableView(dmv.parent)
        val filesInDir = dmv.childNodes
        val javaSourceFiles = filesInDir collect {
          case f: FileArtifactBackedMutableView if f.path.endsWith(JavaExtension) =>
            new JavaSourceMutableView(f.originalBackingObject, jpmv)
        }
        val allClasses = javaSourceFiles.flatMap(s => s.children(JavaTypeAlias))
        Some(allClasses)
      case _ => None
    }

}

object JavaTypeType {

  def annotationAddedTo(bd: BodyDeclaration, annotationName: String): Boolean = {
    val newAnnotation = new MarkerAnnotationExpr(new NameExpr(annotationName))
    if (!bd.getAnnotations.asScala.map(ann => ann.getName).contains(newAnnotation.getName)) {
      bd.setAnnotations((bd.getAnnotations.asScala :+ newAnnotation).asJava)
      true
    } else // It's already there
      false
  }

  val ConstructorAlias: String = "constructor"

  val FieldAlias: String = "field"

  val MethodAlias: String = "method"

  val JavaExtension: String = ".java"

  val JavaTypeAlias: String = Typed.typeClassToTypeName(classOf[JavaTypeType])

}
