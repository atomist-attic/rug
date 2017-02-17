package com.atomist.rug.kind.java

import java.util.{List => JList}

import com.atomist.graph.GraphNode
import com.atomist.rug.kind.core.ProjectMutableView
import com.atomist.rug.kind.java.support._
import com.atomist.rug.kind.support.ProjectDecoratingMutableView
import com.atomist.rug.runtime.rugdsl.{DefaultEvaluator, Evaluator}
import com.atomist.rug.spi._
import com.atomist.source.FileArtifact
import com.atomist.tree.TreeNode
import com.atomist.util.lang.JavaHelpers
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration

import scala.collection.JavaConverters._

class JavaProjectType(
                       evaluator: Evaluator
                                  )
  extends Type(evaluator)
    with ReflectivelyTypedType {

  def this() = this(DefaultEvaluator)

  override def description = "Java project"

  override def runtimeClass = classOf[JavaProjectMutableView]

  override def findAllIn(context: GraphNode): Option[Seq[MutableView[_]]] = {
    context match {
      case pv: ProjectMutableView if JavaAssertions.isJava(pv.currentBackingObject) =>
        Some(Seq(new JavaProjectMutableView(pv)))
      case _ => Some(Nil)
    }
  }
}

import com.atomist.rug.kind.java.JavaSourceType._

/**
  * Exposes Java project status, allowing refactoring, tests for
  * Maven and Spring usage etc.
  */
class JavaProjectMutableView(pmv: ProjectMutableView)
  extends ProjectDecoratingMutableView(pmv) {

  import JavaProjectMutableView._

  @ExportFunction(readOnly = true, description = "Return the number of Java files in this module")
  def javaFileCount: Int = currentBackingObject.allFiles.count(f => f.name.endsWith(".java"))

  @ExportFunction(readOnly = true, description = "Is this a Maven project?")
  def isMaven: Boolean = JavaAssertions.isMaven(currentBackingObject)

  @ExportFunction(readOnly = true, description = "Is this a Spring project?")
  def isSpring: Boolean = JavaAssertions.isSpring(currentBackingObject)

  @ExportFunction(readOnly = true, description = "Is this a Spring Boot project?")
  def isSpringBoot: Boolean = JavaAssertions.isSpringBoot(currentBackingObject)

  @ExportFunction(readOnly = true, description = "List the packages in this project")
  def packages: JList[PackageInfo] = {
    PackageFinder.packages(currentBackingObject).asJava
  }

  @ExportFunction(readOnly = false, description = "Rename the given package. All package under it will also be renamed")
  def renamePackage(@ExportFunctionParameterDescription(
    name = "oldPackage",
    description = "Old package name")
                    oldPackageName: String,
                    @ExportFunctionParameterDescription(
                      name = "newPackage",
                      description = "The new package name")
                    newPackageName: String): Unit = {
    // We do this will string operations rather than JavaParser
    if (!JavaHelpers.isValidPackageName(newPackageName))
      fail(s"Invalid new package name: [$newPackageName]")
    val pathToReplace = oldPackageName.replace(".", "/")
    val newPath = newPackageName.replace(".", "/")

    // Replace imports and other references, such as annotations and Strings
    //regexpReplaceWithFilter(isJavaFilter, s"import[\\s]+$oldPackageName", s"import $newPackageName")
    regexpReplaceWithFilter(mayContainReferencesToClassNames, oldPackageName, newPackageName)

    regexpReplaceWithFilter(f => f.name.endsWith(JavaExtension) && f.path.contains(pathToReplace),
      s"package[\\s]+$oldPackageName", s"package $newPackageName")

    // Fix file structure
    replaceInPath(pathToReplace, newPath)
  }

  def javaSourceViews: Seq[JavaSourceMutableView] =
    currentBackingObject.allFiles
      .filter(_.name.endsWith(JavaExtension))
      .view
      .map(f => new JavaSourceMutableView(f, this))

  def compilationUnits: Seq[CompilationUnit] =
    javaSourceViews.flatMap(jsv => jsv.compilationUnit)

  def classes: Seq[ClassOrInterfaceDeclaration] =
    javaSourceViews.flatMap(_.compilationUnit).flatMap(cu => cu.getTypes.asScala collect {
      case coit: ClassOrInterfaceDeclaration => coit
    })
}

import com.atomist.rug.kind.core.ProjectType.ProvenanceFilePath

object JavaProjectMutableView {

  val isJava: FileArtifact => Boolean = f => f.name.endsWith(JavaExtension)

  val mayContainReferencesToClassNames: FileArtifact => Boolean = f =>
    !f.path.equals(ProvenanceFilePath)

  def apply(pv: ProjectMutableView) = pv match {
    case jpv: JavaProjectMutableView => jpv
    case pv: ProjectMutableView => new JavaProjectMutableView(pv)
    case x => throw new IllegalArgumentException(s"Cannot convert $x to a JavaProjectMutableView")
  }
}
