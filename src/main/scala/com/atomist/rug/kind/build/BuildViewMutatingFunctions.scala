package com.atomist.rug.kind.build

import com.atomist.rug.spi.{ExportFunction, ExportFunctionParameterDescription}

/**
  * Common view for mutating functions on build
  */
trait BuildViewMutatingFunctions {

  @ExportFunction(readOnly = false, description = "Set the content of the groupId element")
  def setGroupId(@ExportFunctionParameterDescription(name = "newGroupId",
    description = "The groupId that you are trying to set")
                 newGroupId: String): Unit

  @ExportFunction(readOnly = false, description = "Set the content of the artifactId element")
  def setArtifactId(@ExportFunctionParameterDescription(name = "newArtifactId",
    description = "The artifactId that you are trying to set")
                    newArtifactId: String): Unit

  @ExportFunction(readOnly = false, description = "Set the content of the version element")
  def setVersion(@ExportFunctionParameterDescription(name = "newVersion",
    description = "The version that you are trying to set")
                 newVersion: String): Unit

  @ExportFunction(readOnly = false, description = "Add or replace project name")
  def setProjectName(@ExportFunctionParameterDescription(name = "newName",
    description = "The name being set")
                     newName: String): Unit

  @ExportFunction(readOnly = false, description = "Set the content of the description element")
  def setDescription(@ExportFunctionParameterDescription(name = "newDescription",
    description = "The description that you are trying to set")
                     newDescription: String): Unit

  @ExportFunction(readOnly = false, description = "Add or replace a property")
  def addOrReplaceProperty(@ExportFunctionParameterDescription(name = "propertyName",
    description = "The name of the property being set")
                           propertyName: String,
                           @ExportFunctionParameterDescription(name = "propertyValue",
                             description = "The value of the property being set")
                           propertyValue: String): Unit

  @ExportFunction(readOnly = false, description = "Remove a property")
  def removeProperty(@ExportFunctionParameterDescription(name = "propertyName",
    description = "The name of the project property being deleted")
                     propertyName: String): Unit

  @ExportFunction(readOnly = false, description = "Add or replace a dependency")
  def addOrReplaceDependency(@ExportFunctionParameterDescription(name = "groupId",
    description = "The value of the dependency's groupId")
                             groupId: String,
                             @ExportFunctionParameterDescription(name = "artifactId",
                               description = "The value of the dependency's artifactId")
                             artifactId: String): Unit

  @ExportFunction(readOnly = false, description = "Add or replace a dependency, providing version")
  def addOrReplaceDependencyOfVersion(@ExportFunctionParameterDescription(name = "groupId",
    description = "The value of the dependency's groupId")
                                      groupId: String,
                                      @ExportFunctionParameterDescription(name = "artifactId",
                                        description = "The value of the dependency's artifactId")
                                      artifactId: String,
                                      @ExportFunctionParameterDescription(name = "newVersion",
                                        description = "The value of the dependency's version to be set")
                                      version: String): Unit

  @ExportFunction(readOnly = false, description = "Add or replace a dependency's version")
  def addOrReplaceDependencyVersion(@ExportFunctionParameterDescription(name = "groupId",
    description = "The value of dependency's groupId")
                                    groupId: String,
                                    @ExportFunctionParameterDescription(name = "artifactId",
                                      description = "The value of the dependency's artifactId")
                                    artifactId: String,
                                    @ExportFunctionParameterDescription(name = "newVersion",
                                      description = "The value of the dependency's version to be set")
                                    newVersion: String): Unit

  @ExportFunction(readOnly = false, description = "Remove a dependency's version")
  def removeDependencyVersion(@ExportFunctionParameterDescription(name = "groupId",
    description = "The value of the dependency's groupId")
                              groupId: String,
                              @ExportFunctionParameterDescription(name = "artifactId",
                                description = "The value of the dependency's artifactId")
                              artifactId: String): Unit

  @ExportFunction(readOnly = false, description = "Removes a dependency")
  def removeDependency(@ExportFunctionParameterDescription(name = "groupId",
    description = "The value of the dependency's groupId")
                       groupId: String,
                       @ExportFunctionParameterDescription(name = "artifactId",
                         description = "The value of the dependency's artifactId")
                       artifactId: String): Unit
}
