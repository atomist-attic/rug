package com.atomist.rug.kind.build

import com.atomist.rug.spi.{ExportFunction, ExportFunctionParameterDescription}

/**
  * Provides a consistent view on top of all build tools
  */
trait BuildViewNonMutatingFunctions {

  @ExportFunction(readOnly = true, description = "Return the content of the groupId element")
  def groupId: String

  @ExportFunction(readOnly = true, description = "Return the content of the artifactId element")
  def artifactId: String

  @ExportFunction(readOnly = true, description = "Return the content of the version element")
  def version: String

  @ExportFunction(readOnly = true, description = "Return the content of the name element")
  def name: String

  @ExportFunction(readOnly = true, description = "Return the content of the description element")
  def description: String

  @ExportFunction(readOnly = true, description = "Return the value of a project property")
  def property(@ExportFunctionParameterDescription(name = "projectPropertyName",
    description = "The project property you are looking to inspect")
               projectPropertyName: String): String

  @ExportFunction(readOnly = true, description = "Return the value of a dependency's version as specified by artifactId")
  def dependencyVersion(@ExportFunctionParameterDescription(name = "groupId",
    description = "The groupId of the dependency you are looking to inspect")
                        groupId: String,
                        @ExportFunctionParameterDescription(name = "artifactId",
                          description = "The artifactId of the dependency you are looking to inspect")
                        artifactId: String): String

  @ExportFunction(readOnly = true, description = "Return the value of a dependency's scope as specified by artifactId")
  def dependencyScope(@ExportFunctionParameterDescription(name = "groupId",
    description = "The groupId of the dependency you are looking to inspect")
                      groupId: String,
                      @ExportFunctionParameterDescription(name = "artifactId",
                        description = "The artifactId of the dependency you are looking to inspect")
                      artifactId: String): String

  @ExportFunction(readOnly = true, description = "Return whether a dependency is present as specified by artifactId and groupId")
  def isDependencyPresent(@ExportFunctionParameterDescription(name = "groupId",
    description = "The groupId of the dependency you are looking to test the presence of")
                          groupId: String,
                          @ExportFunctionParameterDescription(name = "artifactId",
                            description = "The artifactId of the dependency you are looking to test the presence of")
                          artifactId: String): Boolean

}
