package com.atomist.rug.kind.java

import com.atomist.project.ProjectOperationArguments
import com.atomist.rug.kind.core.{DirectoryArtifactMutableView, FileArtifactBackedMutableView, ProjectMutableView}
import com.atomist.rug.kind.dynamic.{ChildResolver, ContextlessViewFinder}
import com.atomist.rug.kind.java.JavaClassType._
import com.atomist.rug.kind.java.support.JavaHelpers
import com.atomist.rug.parser.Selected
import com.atomist.rug.runtime.{DefaultEvaluator, Evaluator}
import com.atomist.rug.spi._
import com.atomist.source.ArtifactSource
import com.github.javaparser.ast.body._
import com.github.javaparser.ast.expr.{MarkerAnnotationExpr, NameExpr}

import scala.collection.JavaConversions._

class JavaClassType(evaluator: Evaluator)
  extends Type(evaluator)
    with ContextlessViewFinder
    with ReflectivelyTypedType {

  def this() = this(DefaultEvaluator)

  override def name = JavaTypeAlias

  override val resolvesFromNodeTypes: Set[String] = Set("project", "directory", "file")

  override def description = "Java class"

  override def viewManifest: Manifest[JavaClassOrInterfaceView] = manifest[JavaClassOrInterfaceView]

  override protected def findAllIn(rugAs: ArtifactSource, selected: Selected, context: MutableView[_],
                                   poa: ProjectOperationArguments, identifierMap: Map[String, Object]): Option[Seq[MutableView[_]]] =
    context match {
      case pv: ProjectMutableView =>
        Some(JavaProjectMutableView(pv).javaSourceViews.map(_.defaultChildViews).flatten)
      case fmv: FileArtifactBackedMutableView =>
        Some(
          Seq(new JavaSourceMutableView(fmv.originalBackingObject, JavaProjectMutableView(fmv.parent))).flatMap(s => s.children(JavaTypeAlias))
        )
      case dmv: DirectoryArtifactMutableView =>
        val jpmv = JavaProjectMutableView(dmv.parent)
        //println(s"childrenNames* are ${dmv.childrenNames},childNodeNames=${dmv.childNodeNames}")
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

object JavaClassType {

  def annotationAddedTo(bd: BodyDeclaration, annotationName: String): Boolean = {
    val newAnnotation = new MarkerAnnotationExpr(new NameExpr(annotationName))
    if (!bd.getAnnotations.map(ann => ann.getName).contains(newAnnotation.getName)) {
      bd.setAnnotations(bd.getAnnotations :+ newAnnotation)
      true
    } else // It's already there
      false
  }

  val JavaTypeAlias = "java.class"

  val ConstructorAlias = "constructor"

  val FieldAlias = "field"

  val MethodAlias = "method"

  val JavaExtension = ".java"
}
