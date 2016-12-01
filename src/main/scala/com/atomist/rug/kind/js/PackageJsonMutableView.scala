package com.atomist.rug.kind.js

import com.atomist.rug.kind.core.{FileArtifactMutableView, ProjectMutableView}
import com.atomist.rug.runtime.lang.js.NashornExpressionEngine
import com.atomist.rug.runtime.rugdsl.FunctionInvocationContext
import com.atomist.rug.spi.{ExportFunction, ExportFunctionParameterDescription}
import com.atomist.source.FileArtifact

/**
  * Mutable view for an Elm module exposed to Rug and JavaScript
  *
  * @param originalBackingObject start state of the file
  * @param parent                owning Elm project
  */
class PackageJsonMutableView(
                              originalBackingObject: FileArtifact,
                              parent: ProjectMutableView)
  extends FileArtifactMutableView(originalBackingObject, parent) {

  // TODO we could probably make this more efficient by not using JavaScript

  @ExportFunction(readOnly = false, description = "Change the package name")
  def setPackageName(@ExportFunctionParameterDescription(name = "newName",
    description = "The name to set the package to")
                     newName: String,
                     @ExportFunctionParameterDescription(name = "ic",
                       description = "")
                     ic: FunctionInvocationContext[PackageJsonMutableView]): Unit = {
    val expr =
      s"""
         |var pkg = JSON.parse(${ic.targetAlias}.content());
         |pkg.name = "Foo";
         |return JSON.stringify(pkg, null, 4);
      """.stripMargin
    val r = NashornExpressionEngine.evaluator[PackageJsonMutableView](ic, expr).evaluate(ic)
    setContent(r.toString)
  }

  @ExportFunction(readOnly = true, description = "Return package name")
  def packageName(@ExportFunctionParameterDescription(name = "ic",
    description = "")
                  ic: FunctionInvocationContext[PackageJsonMutableView]): String = {
    val expr =
      s"""
         |var pkg = JSON.parse(${ic.targetAlias}.content());
         |return pkg.name;
      """.stripMargin
    val r = NashornExpressionEngine.evaluator[PackageJsonMutableView](ic, expr).evaluate(ic)
    r.toString
  }

  @ExportFunction(readOnly = false, description = "Return package name")
  def addDependency(name: String, version: String,
                    ic: FunctionInvocationContext[PackageJsonMutableView]): String = {
    val expr =
      s"""
         |var pkg = JSON.parse(${ic.targetAlias}.content());
         |pkg.dependencies[name] = version;
         |return JSON.stringify(pkg, null, 4);
      """.stripMargin
    val r = NashornExpressionEngine.evaluator[PackageJsonMutableView](ic, expr).evaluate(ic)
    r.toString
  }
}
