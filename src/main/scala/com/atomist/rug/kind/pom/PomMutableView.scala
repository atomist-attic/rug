package com.atomist.rug.kind.pom

import com.atomist.rug.kind.build.{BuildViewMutatingFunctions, BuildViewNonMutatingFunctions}
import com.atomist.rug.kind.core.ProjectMutableView
import com.atomist.rug.kind.xml.XmlMutableView
import com.atomist.rug.spi.{ExportFunction, ExportFunctionParameterDescription, TerminalView}
import com.atomist.source.FileArtifact

object PomMutableView {

  val project = "project"

  val projectBaseXPath = s"/$project"

  val mavenGroupId = "groupId"

  val mavenArtifactId = "artifactId"

  val groupIdXPath = s"$projectBaseXPath/$mavenGroupId"

  val artifactIdXPath = s"$projectBaseXPath/$mavenArtifactId"

  val version = "version"

  val versionXPath = s"$projectBaseXPath/$version"

  val scope = "scope"

  val packagingXPath = s"$projectBaseXPath/packaging"

  val name = "name"

  val nameXPath = s"$projectBaseXPath/$name"

  val descriptionXPath = s"$projectBaseXPath/description"

  val parent = "parent"

  val parentBaseXPath = s"$projectBaseXPath/$parent"

  val parentGroupIdXPath = s"$parentBaseXPath/$mavenGroupId"

  val parentArtifactIdXPath = s"$parentBaseXPath/$mavenArtifactId"

  val parentVersionXPath = s"$parentBaseXPath/version"

  val projectPropertyBaseXPath = s"$projectBaseXPath/properties"

  val dependency = "dependency"

  val dependencies = "dependencies"

  val dependenciesBaseXPath = s"$projectBaseXPath/$dependencies"

  val dependencyBaseXPath = s"$projectBaseXPath/$dependencies/$dependency"

  val plugin = "plugin"

  val buildPluginsBaseXPath = s"$projectBaseXPath/build/plugins"

  val dependencyManagement = "dependencyManagement"

  val dependencyManagementBaseXPath = s"$projectBaseXPath/$dependencyManagement"
}

trait PomMutableViewNonMutatingFunctions extends BuildViewNonMutatingFunctions {

  import PomMutableView._

  def getTextContentFor(xpath: String): String

  def contains(xpath: String): Boolean

  @ExportFunction(readOnly = true, description = "Return the content of the groupId element")
  def groupId: String = getTextContentFor(groupIdXPath)

  @ExportFunction(readOnly = true, description = "Return the content of the artifactId element")
  def artifactId: String = getTextContentFor(artifactIdXPath)

  @ExportFunction(readOnly = true, description = "Return the content of the version element")
  def version: String = getTextContentFor(versionXPath)

  @ExportFunction(readOnly = true, description = "Return the content of the packaging element")
  def packaging: String = getTextContentFor(packagingXPath)

  @ExportFunction(readOnly = true, description = "Return the content of the name element")
  def name: String = getTextContentFor(nameXPath)

  @ExportFunction(readOnly = true, description = "Return the content of the description element")
  def description: String = getTextContentFor(descriptionXPath)

  @ExportFunction(readOnly = true, description = "Return the content of the parent groupId")
  def parentGroupId: String = getTextContentFor(parentGroupIdXPath)

  @ExportFunction(readOnly = true, description = "Return the content of the parent artifactId")
  def parentArtifactId: String = getTextContentFor(parentArtifactIdXPath)

  @ExportFunction(readOnly = true, description = "Return the content of the parent version")
  def parentVersion: String = getTextContentFor(parentVersionXPath)

  @ExportFunction(readOnly = true, description = "Return the value of a project property")
  def property(@ExportFunctionParameterDescription(name = "projectPropertyName",
    description = "The project property you are looking to inspect")
               projectPropertyName: String): String =
    getTextContentFor(s"$projectPropertyBaseXPath/$projectPropertyName")

  @ExportFunction(readOnly = true, description = "Return the value of a dependency's version as specified by artifactId")
  def dependencyVersion(@ExportFunctionParameterDescription(name = "groupId",
    description = "The groupId of the dependency you are looking to inspect")
                        groupId: String,
                        @ExportFunctionParameterDescription(name = "artifactId",
                          description = "The artifactId of the dependency you are looking to inspect")
                        artifactId: String): String =
    getTextContentFor(s"$dependencyBaseXPath/version[../$mavenArtifactId = '$artifactId'and ../$mavenGroupId [text() = '$groupId']]")

  @ExportFunction(readOnly = true, description = "Return the value of a dependency's scope as specified by artifactId")
  def dependencyScope(@ExportFunctionParameterDescription(name = "groupId",
    description = "The groupId of the dependency you are looking to inspect")
                      groupId: String,
                      @ExportFunctionParameterDescription(name = "artifactId",
                        description = "The artifactId of the dependency you are looking to inspect")
                      artifactId: String): String =
    getTextContentFor(s"$dependencyBaseXPath/$scope[../$mavenArtifactId = '$artifactId'and ../$mavenGroupId [text() = '$groupId']]")

  @ExportFunction(readOnly = true, description = "Return whether a dependency is present as specified by artifactId and groupId")
  def isDependencyPresent(@ExportFunctionParameterDescription(name = "groupId",
    description = "The groupId of the dependency you are looking to test the presence of")
                          groupId: String,
                          @ExportFunctionParameterDescription(name = "artifactId",
                            description = "The artifactId of the dependency you are looking to test the presence of")
                          artifactId: String): Boolean =
    contains(s"$dependencyBaseXPath/$mavenArtifactId[text() = '$artifactId' and ../$mavenGroupId [text() = '$groupId']]")

  @ExportFunction(readOnly = true, description = "Return whether a build plugin is present as specified by artifactId and groupId")
  def isBuildPluginPresent(@ExportFunctionParameterDescription(name = "groupId",
    description = "The groupId of the build plugin you are looking to test the presence of")
                           groupId: String,
                           @ExportFunctionParameterDescription(name = "artifactId",
    description = "The artifactId of the build plugin you are looking to test the presence of")
                           artifactId: String): Boolean =
    contains(s"$buildPluginsBaseXPath/$plugin/$mavenArtifactId [text() = '$artifactId' and ../$mavenGroupId [text() = '$groupId']]")

  @ExportFunction(readOnly = true, description = "Return whether a dependency management dependency is present as specified by artifactId and groupId")
  def isDependencyManagementDependencyPresent(@ExportFunctionParameterDescription(name = "groupId",
    description = "The groupId of the dependency management dependency you are looking to test the presence of")
                                              groupId: String,
                                              @ExportFunctionParameterDescription(name = "artifactId",
    description = "The artifactId of the dependency management dependency you are looking to test the presence of")
                                              artifactId: String): Boolean =
    contains(s"$dependencyManagementBaseXPath/$dependencies/$dependency/$mavenArtifactId [text() = '$artifactId' and ../$mavenGroupId [text() = '$groupId']]")

}

trait PomMutableViewMutatingFunctions extends BuildViewMutatingFunctions {

  import PomMutableView._

  def setTextContentFor(xpath: String, newContent: String): Unit

  def addOrReplaceNode(xpathToParent: String, xpathToNode: String, newNode: String, nodeContent: String): Unit

  def deleteNode(xpath: String): Unit

  @ExportFunction(readOnly = false, description = "Set the content of the groupId element")
  def setGroupId(@ExportFunctionParameterDescription(name = "newGroupId",
    description = "The groupId that you are trying to set")
                 newGroupId: String): Unit = setTextContentFor(groupIdXPath, newGroupId)

  @ExportFunction(readOnly = false, description = "Set the content of the artifactId element")
  def setArtifactId(@ExportFunctionParameterDescription(name = "newArtifactId",
    description = "The artifactId that you are trying to set")
                    newArtifactId: String): Unit = setTextContentFor(artifactIdXPath, newArtifactId)

  @ExportFunction(readOnly = false, description = "Set the content of the version element")
  def setVersion(@ExportFunctionParameterDescription(name = "newVersion",
    description = "The version that you are trying to set")
                 newVersion: String): Unit = setTextContentFor(versionXPath, newVersion)

  @ExportFunction(readOnly = false, description = "Set the content of the packaging element")
  def setPackaging(@ExportFunctionParameterDescription(name = "newPackaging",
    description = "The packaging that you are trying to set")
                   newPackaging: String): Unit = setTextContentFor(packagingXPath, newPackaging)

  @ExportFunction(readOnly = false, description = "Add or replace project name")
  def setProjectName(@ExportFunctionParameterDescription(name = "newName",
    description = "The name being set")
                           newName: String): Unit =
    addOrReplaceNode(s"$projectPropertyBaseXPath",
      nameXPath,
      name,
      s"<$name>$newName</$name>")

  @ExportFunction(readOnly = false, description = "Set the content of the description element")
  def setDescription(@ExportFunctionParameterDescription(name = "newDescription",
    description = "The description that you are trying to set")
                     newDescription: String): Unit = setTextContentFor(descriptionXPath, newDescription)

  @ExportFunction(readOnly = false, description = "Set the content of the parent groupId element")
  def setParentGroupId(@ExportFunctionParameterDescription(name = "newParentGroupId",
    description = "The parent groupId that you are trying to set")
                       newParentGroupId: String): Unit = setTextContentFor(parentGroupIdXPath, newParentGroupId)

  @ExportFunction(readOnly = false, description = "Set the content of the parent artifactId element")
  def setParentArtifactId(@ExportFunctionParameterDescription(name = "newParentArtifactId",
    description = "The parent artifactId that you are trying to set")
                          newParentArtifactId: String): Unit = setTextContentFor(parentArtifactIdXPath, newParentArtifactId)

  @ExportFunction(readOnly = false, description = "Set the content of the parent version element")
  def setParentVersion(@ExportFunctionParameterDescription(name = "newParentVersion",
    description = "The parent version that you are trying to set")
                       newParentVersion: String): Unit = setTextContentFor(parentVersionXPath, newParentVersion)

  @ExportFunction(readOnly = false, description = "Set the content of the parent block")
  def replaceParent(@ExportFunctionParameterDescription(name = "newParentBlock",
    description = "The parent block that you are trying to set")
                    newParentBlock: String): Unit = addOrReplaceNode(parentBaseXPath, parentBaseXPath, parent, newParentBlock)

  @ExportFunction(readOnly = false, description = "Add or replace a property")
  def addOrReplaceProperty(@ExportFunctionParameterDescription(name = "propertyName",
    description = "The name of the property being set")
                           propertyName: String,
                           @ExportFunctionParameterDescription(name = "propertyValue",
                             description = "The value of the property being set")
                           propertyValue: String): Unit =
    addOrReplaceNode(s"$projectPropertyBaseXPath",
      s"/project/properties/$propertyName",
      propertyName,
      s"<$propertyName>$propertyValue</$propertyName>")

  @ExportFunction(readOnly = false, description = "Remove a property")
  def removeProperty(@ExportFunctionParameterDescription(name = "propertyName",
    description = "The name of the project property being deleted")
                     propertyName: String): Unit =
    deleteNode(s"$projectPropertyBaseXPath/$propertyName")

  @ExportFunction(readOnly = false, description = "Add or replace a dependency")
  def addOrReplaceDependency(@ExportFunctionParameterDescription(name = "groupId",
    description = "The value of the dependency's groupId")
                             groupId: String,
                             @ExportFunctionParameterDescription(name = "artifactId",
                               description = "The value of the dependency's artifactId")
                             artifactId: String): Unit =
    addOrReplaceNode(dependenciesBaseXPath,
      s"/project/dependencies/dependency/artifactId[text()='$artifactId' and ../groupId[text() = '$groupId']]/..",
      dependency,
      s"""<dependency><groupId>$groupId</groupId><artifactId>$artifactId</artifactId></dependency>""")

  @ExportFunction(readOnly = false, description = "Add or replace a dependency, providing version")
  def addOrReplaceDependencyOfVersion(@ExportFunctionParameterDescription(name = "groupId",
    description = "The value of the dependency's groupId")
                             groupId: String,
                             @ExportFunctionParameterDescription(name = "artifactId",
                               description = "The value of the dependency's artifactId")
                             artifactId: String,
                                      @ExportFunctionParameterDescription(name = "newVersion",
                                        description = "The value of the dependency's version to be set")
                                      version: String): Unit =
    addOrReplaceNode(dependenciesBaseXPath,
      s"/project/dependencies/dependency/artifactId[text()='$artifactId' and ../groupId[text() = '$groupId']]/..",
      dependency,
      s"""<dependency><groupId>$groupId</groupId><artifactId>$artifactId</artifactId><version>$version</version></dependency>""")

  @ExportFunction(readOnly = false, description = "Add or replace a dependency's version")
  def addOrReplaceDependencyVersion(@ExportFunctionParameterDescription(name = "groupId",
    description = "The value of dependency's groupId")
                                    groupId: String,
                                    @ExportFunctionParameterDescription(name = "artifactId",
                                      description = "The value of the dependency's artifactId")
                                    artifactId: String,
                                    @ExportFunctionParameterDescription(name = "newVersion",
                                      description = "The value of the dependency's version to be set")
                                    newVersion: String): Unit =
    addOrReplaceDependencySubNode(artifactId, groupId, version, newVersion)

  @ExportFunction(readOnly = false, description = "Remove a dependency's version")
  def removeDependencyVersion(@ExportFunctionParameterDescription(name = "groupId",
    description = "The value of the dependency's groupId")
                              groupId: String,
                              @ExportFunctionParameterDescription(name = "artifactId",
                                description = "The value of the dependency's artifactId")
                              artifactId: String): Unit =
    removeDependencySubNode(artifactId, groupId, version)

  @ExportFunction(readOnly = false, description = "Add or replace a dependency's scope")
  def addOrReplaceDependencyScope(@ExportFunctionParameterDescription(name = "groupId",
    description = "The value of the dependency's groupId")
                                  groupId: String,
                                  @ExportFunctionParameterDescription(name = "artifactId",
                                    description = "The value of the dependency's artifactId")
                                  artifactId: String,
                                  @ExportFunctionParameterDescription(name = "newScope",
                                    description = "The new value of the dependency's scope to be set")
                                  newScope: String): Unit =
    addOrReplaceDependencySubNode(artifactId, groupId, scope, newScope)

  @ExportFunction(readOnly = false, description = "Remove a dependency's scope")
  def removeDependencyScope(@ExportFunctionParameterDescription(name = "groupId",
    description = "The value of the dependency's groupId")
                            groupId: String,
                            @ExportFunctionParameterDescription(name = "artifactId",
                              description = "The value of the dependency's artifactId")
                            artifactId: String): Unit =
    removeDependencySubNode(artifactId, groupId, scope)

  @ExportFunction(readOnly = false, description = "Removes a dependency")
  def removeDependency(@ExportFunctionParameterDescription(name = "groupId",
    description = "The value of the dependency's groupId")
                       groupId: String,
                       @ExportFunctionParameterDescription(name = "artifactId",
                         description = "The value of the dependency's artifactId")
                       artifactId: String): Unit =
    deleteNode(s"/project/dependencies/dependency/artifactId[text()='$artifactId' and ../groupId[text() = '$groupId']]/..")

  private def removeDependencySubNode(artifactId: String, groupId: String, subnode: String): Unit =
    deleteNode(s"/project/dependencies/dependency/artifactId[text()='$artifactId' and ../groupId[text() = '$groupId']]/../$subnode")

  private def addOrReplaceDependencySubNode(artifactId: String, groupId: String, subnode: String, content: String): Unit =
    addOrReplaceNode(dependencyBaseXPath,
      s"/project/dependencies/dependency/artifactId[text()='$artifactId' and ../groupId[text() = '$groupId']]/../$subnode",
      subnode,
      s"""<$subnode>$content</$subnode>""")

  @ExportFunction(readOnly = false, description = "Adds or replaces a build plugin")
  def addOrReplaceBuildPlugin(@ExportFunctionParameterDescription(name = "groupId",
    description = "The value of the build plugin's groupId")
                              groupId: String,
                              @ExportFunctionParameterDescription(name = "artifactId",
                                description = "The value of the build plugin's artifactId")
                              artifactId: String,
                              @ExportFunctionParameterDescription(name = "pluginContent",
                                description = "The XML content for the plugin")
                              pluginContent: String): Unit =
    addOrReplaceNode(buildPluginsBaseXPath,
      s"/project/build/plugins/plugin/artifactId [text()='$artifactId' and ../groupId [text() = '$groupId']]/..",
      plugin,
      pluginContent)

  @ExportFunction(readOnly = false, description = "Adds or replaces a dependency management dependency")
  def addOrReplaceDependencyManagementDependency(@ExportFunctionParameterDescription(name = "groupId",
    description = "The value of the dependency's groupId")
                                                 groupId: String,
                                                 @ExportFunctionParameterDescription(name = "artifactId",
                                                   description = "The value of the dependency's artifactId")
                                                 artifactId: String,
                                                 @ExportFunctionParameterDescription(name = "dependencyContent",
                                                   description = "The XML content for the dependency")
                                                 dependencyContent: String): Unit = {
    addDependencyManagementSectionIfNotPresent()

    addOrReplaceNode(s"$dependencyManagementBaseXPath/$dependencies",
      s"/$dependencyManagementBaseXPath/$dependencies/$dependency/$mavenArtifactId [text()='$artifactId' and ../$mavenGroupId [text() = '$groupId']]/..",
      dependency,
      dependencyContent)
  }

  private def addDependencyManagementSectionIfNotPresent(): Unit = {
    addOrReplaceNode(projectBaseXPath,
      dependencyManagementBaseXPath,
      dependencyManagement,
      s"<$dependencyManagement><$dependencies></$dependencies></$dependencyManagement>")
  }
}

class PomMutableView(
                      originalBackingObject: FileArtifact,
                      parent: ProjectMutableView)
  extends XmlMutableView(originalBackingObject, parent)
    with TerminalView[FileArtifact]
    with PomMutableViewNonMutatingFunctions
    with PomMutableViewMutatingFunctions
