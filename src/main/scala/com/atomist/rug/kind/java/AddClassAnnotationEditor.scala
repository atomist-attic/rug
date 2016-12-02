package com.atomist.rug.kind.java

import com.atomist.project.ProjectOperationArguments
import com.atomist.rug.kind.java.TypeSelection.TypeSelector
import com.atomist.source.ArtifactSource
import com.atomist.util.lang.{JavaConstants, JavaHelpers, JavaParserUtils}
import com.github.javaparser.JavaParser
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.typesafe.scalalogging.LazyLogging

import scala.collection.JavaConversions._

/**
  * Add a class annotation if it doesn't exist.
  *
  * @param selector selects class to annotate
  * @param annotationPackageName Annotation package None means default package
  */
// TODO this supports only marker annotations: Need superclass to generalize
class AddClassAnnotationEditor(selector: TypeSelector,
                               annotationPackageName: Option[String],
                               annotationName: String,
                               javaSourcePath: String = JavaConstants.DefaultBaseSourcePath)
  extends JavaParserProjectEditor("AddClassAnnotation", javaSourcePath)
    with LazyLogging {

  import JavaParserProjectEditor._

  private def annotationFqn: String =
    if (annotationPackageName.isEmpty)
      annotationName
    else
      annotationPackageName.get + "." + annotationName

  // Could pull into superclass, using Parser
  override def meetsPostcondition(as: ArtifactSource): Boolean = {
    val annotatedFiles = as.files
      .filter(JavaHelpers.isJavaSourceArtifact)
      .exists(f => {
        val cu = JavaParser.parse(f.inputStream())
        cu.getTypes.exists(t => t.getAnnotations.exists(ann => ann.getName.getName.equals(annotationName)))
      })
    annotatedFiles
  }

  override protected def maybeModifyCompilationUnit(cu: CompilationUnit, poa: ProjectOperationArguments): Option[CompilationUnit] = {
    val modifiedTypes: Traversable[ClassOrInterfaceDeclaration] = cu.getTypes collect {
      case coit: ClassOrInterfaceDeclaration if selector(coit) && JavaClassType.annotationAddedTo(coit, annotationName) =>
        coit
    }

    if (modifiedTypes.nonEmpty) {
      if (annotationPackageName.isDefined)
        JavaParserUtils.addImportsIfNeeded(Seq(annotationFqn), cu)
      Some(cu)
    } else
      None
  }

  override def description(): String = s"Add @$annotationFqn annotation to class"
}
