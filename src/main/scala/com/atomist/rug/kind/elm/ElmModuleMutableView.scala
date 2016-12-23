package com.atomist.rug.kind.elm

import com.atomist.rug.RugRuntimeException
import com.atomist.rug.kind.core.LazyFileArtifactBackedMutableView
import com.atomist.rug.kind.elm.ElmModel.{AllExposing, FunctionNamesExposing}
import com.atomist.rug.spi.{ExportFunctionParameterDescription, _}
import com.atomist.source.FileArtifact
import com.atomist.util.Utils.StringImprovements

/**
  * Mutable view for an Elm module exposed to Rug and JavaScript
  *
  * @param originalBackingObject start state of the file
  * @param parent                owning Elm project
  */
class ElmModuleMutableView(
                            originalBackingObject: FileArtifact,
                            parent: ElmProjectMutableView)
  extends LazyFileArtifactBackedMutableView(originalBackingObject, parent) {

  private lazy val em = ElmParser.parse(originalBackingObject.content)

  def currentView = em

  override protected def currentContent: String = {
    em.currentSource.toSystem
  }

  override def nodeType: String = Typed.typeClassToTypeName(classOf[ElmModuleType])

  override def nodeName: String = name

  @ExportFunction(readOnly = true, description = "Does the module expose this?")
  def exposes(@ExportFunctionParameterDescription(name = "name",
    description = "A function or type that might be exposed")
              name: String): Boolean = {
    em.exposing match {
      case ae: AllExposing =>
        em.declarations.exists(d => d.declaredIdentifier.equals(name))
      case fne: FunctionNamesExposing =>
        fne.names.exists(n => n.value.equals(name))
    }
  }

  @ExportFunction(readOnly = true, description = "Return the name of the module")
  def name = {
    // Use the filename to avoid forcing parsing the file if we haven't yet done soCompuç
    val n = currentBackingObject.name.dropRight(ElmModuleType.ElmExtension.length)
    n
  }

  @ExportFunction(readOnly = false, description = "Change the name of the module")
  def rename(@ExportFunctionParameterDescription(name = "newName",
    description = "The module name to change to")
             newName: String): Unit = {
    logger.debug(s"Renamed module ${em.nodeName} to $newName")
    em.nameField.update(newName)
    this.setName(s"$newName.elm")
  }

  @ExportFunction(readOnly = false, description = "Replace the exposing")
  def replaceExposing(@ExportFunctionParameterDescription(name = "newExposing",
    description = "New content of exposing. Does not include exposing keyword. Will be either a CSV list or ..")
                      newExposing: String): Unit = {
    em.replaceExposing(newExposing)
  }

  @ExportFunction(readOnly = true, description = "Does the module import the given module?")
  def imports(@ExportFunctionParameterDescription(name = "moduleName",
    description = "The module name to check")
              moduleName: String): Boolean = {
    em.imports.exists(imp => imp.moduleName.equals(moduleName))
  }

  @ExportFunction(readOnly = false,
    description = "Update the given module import")
  def updateImport(@ExportFunctionParameterDescription(name = "oldModuleName",
    description = "The old module import name")
                   oldModuleName: String,
                   @ExportFunctionParameterDescription(name = "newName",
                     description = "The module name to change to")
                   newModuleName: String): Unit = {
    em.imports.foreach {
      case imp if imp.moduleName.equals(oldModuleName) => imp.moduleNameField.update(newModuleName)
    }
    logger.debug(s"After update, imports=${em.imports.mkString(",")}")
  }

  @ExportFunction(readOnly = false,
    description = "Add a function with the given declaration")
  def addFunction(
                   @ExportFunctionParameterDescription(name = "body",
                     description = "Body for the function")
                   body: String): Unit = {
    em.addFunctionBody(body)
  }

  @ExportFunction(readOnly = false,
    description = "Remove a function with the given name")
  def removeFunction(
                      @ExportFunctionParameterDescription(name = "name",
                        description = "Name of the function to remove")
                      name: String): Unit = {
    em.removeFunction(name)
  }

  @ExportFunction(readOnly = false,
    description = "Update the given module import")
  def addImportStatement(@ExportFunctionParameterDescription(name = "importStatement",
    description = "The complete import statement")
                         importStatement: String): Unit = {
    em.addImportStatement(importStatement)
    logger.debug(s"After update, imports=${em.imports.mkString(",")}")
  }

  import ElmModuleType._

  override def childNodeNames: Set[String] = Set(FunctionAlias, TypeAliasAlias, TypeAlias, CaseAlias, ImportAlias)

  override def children(fieldName: String): Seq[MutableView[_]] = fieldName match {
    case FunctionAlias => em.functions.map(f => new ElmFunctionMutableView(f, this))
    case TypeAlias => em.types.map(t => new ElmTypeMutableView(t, this))
    case TypeAliasAlias => em.typeAliases.map(ta => new ElmTypeAliasMutableView(ta, this))
    case ImportAlias => em.imports.map(i => new ElmImportMutableView(i, this))
    case CaseAlias => em.functions
      .map(f => new ElmFunctionMutableView(f, this))
      .flatMap(fmv => fmv.children(CaseAlias))
    case x => throw new RugRuntimeException(null, s"Script error: No child with name [$x] of Elm module type")
  }
}
