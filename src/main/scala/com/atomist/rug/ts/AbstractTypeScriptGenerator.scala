package com.atomist.rug.ts

import com.atomist.graph.GraphNode
import com.atomist.param.{Parameter, ParameterValues, Tag}
import com.atomist.project.common.InvalidParametersException
import com.atomist.project.edit._
import com.atomist.project.generate.ProjectGenerator
import com.atomist.rug.runtime.RugSupport
import com.atomist.rug.spi.ReflectiveFunctionExport.exportedOperations
import com.atomist.rug.spi._
import com.atomist.source.{ArtifactSource, FileArtifact, SimpleFileBasedArtifactSource, StringFileArtifact}
import com.atomist.tree.TreeNode
import com.atomist.util.lang.TypeScriptGenerationHelper
import org.apache.commons.lang3.{ClassUtils, StringUtils}

import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer

object AbstractTypeScriptGenerator {

  val DefaultTemplateName = "ts.vm"
  val DefaultFilename = "model/Core.ts"
  val Root = "TreeNode"
}

/**
  * Generate types for documents.
  *
  * @param typeRegistry registry of known Rug Types.
  */
abstract class AbstractTypeScriptGenerator(typeRegistry: TypeRegistry,
                                           config: TypeGenerationConfig,
                                           generateClasses: Boolean,
                                           override val tags: Seq[Tag])
  extends ProjectGenerator
    with ProjectEditor
    with RugSupport {

  import AbstractTypeScriptGenerator._

  val outputPathParam = "output_path"

  protected val indent = "    "
  protected val helper = new TypeScriptGenerationHelper()

  protected case class ParentClassHolder(parent: Class[_], exportedMethods: Seq[MethodInfo])

  private val typeSort: (Typed, Typed) => Boolean = (a, b) => a.name <= b.name

  /**
    * Either an interface or a test class, depending on whether we're generated test classes
    */
  trait GeneratedType {

    def name: String

    def description: String

    def methods: Seq[MethodInfo]

    def parent: Seq[String]
  }

  trait MethodInfo {

    def typeName: String

    def name: String

    def params: Seq[MethodParam]

    def returnType: String

    def description: Option[String]

    def comment: String = {
      val builder = new StringBuilder(s"$indent/**\n")
      builder ++= s"$indent  * ${description.getOrElse("")}\n"
      builder ++= s"$indent  *\n"

      if (params.nonEmpty) {
        for (p <- params)
          yield builder ++= s"$indent  * ${p.comment}\n"
      }

      if (returnType != "void")
        builder ++= s"$indent  * @returns {$returnType}\n"

      builder ++= s"$indent  */\n"
      builder.toString
    }

    def canEqual(a: Any): Boolean = a.isInstanceOf[MethodInfo]

    override def equals(that: Any): Boolean =
      that match {
        case that: MethodInfo => that.canEqual(this) && this.name.hashCode == that.name.hashCode &&
          this.params.hashCode == that.params.hashCode && this.returnType.hashCode == that.returnType.hashCode
        case _ => false
      }

    override def hashCode(): Int =
      Seq(name, params, returnType).map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }

  protected class MethodParam(val name: String, val paramType: String, val description: Option[String]) {

    def canEqual(a: Any): Boolean = a.isInstanceOf[MethodParam]

    override def equals(that: Any): Boolean =
      that match {
        case that: MethodParam => that.canEqual(this) && this.paramType.hashCode == that.paramType.hashCode
        case _ => false
      }

    override def hashCode: Int = paramType.hashCode

    override def toString = s"$name: $paramType"

    def comment: String = s"@param $name {$paramType} ${description.getOrElse("")}"
  }

  protected object MethodParam {

    def apply(name: String, paramType: String, description: Option[String]) = new MethodParam(name, paramType, description)
  }

  override def parameters: Seq[Parameter] = Seq(Parameter(outputPathParam, ".*")
    .setRequired(false)
    .setDisplayName("Path for created doc")
    .setDefaultValue(DefaultFilename))

  @throws[InvalidParametersException](classOf[InvalidParametersException])
  override def generate(projectName: String, poa: ParameterValues): ArtifactSource = {
    val createdFiles = emitTypes(poa)
    new SimpleFileBasedArtifactSource("Rug user model", createdFiles)
  }

  override def modify(as: ArtifactSource, poa: ParameterValues): ModificationAttempt = {
    val createdFile = emitTypes(poa)
    val r = as + createdFile
    SuccessfulModification(r)
  }

  override def applicability(as: ArtifactSource): Applicability = Applicability.OK

  override def description: String = "Generate core Rug type info"

  override def name: String = "TypedDoc"

  protected def getGeneratedTypes(t: Typed, op: TypeOperation): Seq[GeneratedType]

  protected def getSuperInterfaces(op: TypeOperation): List[ParentClassHolder] =
    if (op.definedOn == null) Nil
    else
      ClassUtils.getAllInterfaces(op.definedOn).asScala
        .filterNot(c => classOf[TreeNode] == c || classOf[GraphNode] == c)
        .map(c => ParentClassHolder(c, allMethods(c.getName, exportedOperations(c))))
        .filterNot(_.exportedMethods.isEmpty)
        .toList

  protected def getSuperClasses(op: TypeOperation): List[ParentClassHolder] =
    if (op.definedOn == null) Nil
    else
      ClassUtils.getAllSuperclasses(op.definedOn).asScala
        .filterNot(c => classOf[TreeNode] == c || classOf[GraphNode] == c)
        .map(c => ParentClassHolder(c, allMethods(c.getName, exportedOperations(c))))
        .filterNot(_.exportedMethods.isEmpty)
        .toList

  protected def allMethods(typeName: String, ops: Seq[TypeOperation]): Seq[MethodInfo] = {
    val methods = new ListBuffer[MethodInfo]
    for {
      op <- ops
      if shouldEmit(op)
    } {
      val params =
        for (p <- op.parameters)
          yield
            MethodParam(p.name, helper.javaTypeToTypeScriptType(p.parameterType, typeRegistry), p.description)

      methods += getMethodInfo(typeName, op, params)
    }
    methods.sortBy(_.name)
  }

  protected def getMethodInfo(typeName: String, op: TypeOperation, params: Seq[MethodParam]): MethodInfo

  private def shouldEmit(top: TypeOperation) =
    !(top.parameters.exists(_.parameterType.contains("FunctionInvocationContext")) || "eval".equals(top.name))

  protected def emitDocComment(description: String): String = {
    s"""
       |/*
       | * $description
       | */""".stripMargin
  }

  private def emitTypes(poa: ParameterValues): Seq[FileArtifact] = {
    val tsClassOrInterfaces = ListBuffer.empty[StringFileArtifact]
    val alreadyGenerated = ListBuffer.empty[GeneratedType]
    val generatedTypes = allGeneratedTypes(typeRegistry.types.sortWith(typeSort))
    val pathParam = poa.stringParamValue(outputPathParam)
    val path = if (pathParam.contains("/")) StringUtils.substringBeforeLast(pathParam, "/") + "/" else ""

    for {
      t <- generatedTypes
      if !alreadyGenerated.contains(t)
    } {
      val output = new StringBuilder(config.licenseHeader)
      output ++= config.separator
      output ++= config.imports
      output ++= getImports(generatedTypes, t)
      output ++= s"\nexport {${t.name}}\n"
      output ++= t.toString
      output ++= config.separator
      tsClassOrInterfaces += StringFileArtifact(s"$path${t.name}.ts", output.toString())
      alreadyGenerated += t
    }

    // Add Core.ts
    val output = new StringBuilder(config.licenseHeader)
    output ++= config.separator
    output ++= alreadyGenerated.map(t => s"""import {${t.name}} from "./${t.name}"""").mkString("\n")
    output ++= config.separator
    output ++= alreadyGenerated.map(t => s"export {${t.name}}").mkString("\n")
    tsClassOrInterfaces += StringFileArtifact(pathParam, output.toString())

    tsClassOrInterfaces
  }

  private def allGeneratedTypes(allTypes: Seq[Typed]): Seq[GeneratedType] =
    (allTypes.flatMap(t => t.operations.flatMap(op => getGeneratedTypes(t, op))).groupBy(_.name) map {
      case (_, l) => l.head
    }).toSeq.sortBy(_.name)

  private def getImports(interfaceTypes: Seq[GeneratedType], currentType: GeneratedType) = {
    val imports = interfaceTypes
      .flatMap(t => currentType.methods.map(m => StringUtils.removeEnd(m.returnType, "[]")).filter(currentType.name != t.name && _ == t.name))
      .toList
    (currentType.parent.toList ::: imports).distinct.filter(_ != Root).map(i => s"""import {$i} from "./$i"""").mkString("\n")
  }
}

case class TypeGenerationConfig(
                                 indent: String = "    ",
                                 separator: String = "\n\n",
                                 imports: String = TypeGenerationConfig.DefaultImports
                               )
  extends TypeScriptGenerationConfig {

  val licenseHeader: String =
    """|/*
       | * Copyright 2015-2017 Atomist Inc.
       | *
       | * Licensed under the Apache License, Version 2.0 (the "License");
       | * you may not use this file except in compliance with the License.
       | * You may obtain a copy of the License at
       | *
       | *      http://www.apache.org/licenses/LICENSE-2.0
       | *
       | * Unless required by applicable law or agreed to in writing, software
       | * distributed under the License is distributed on an "AS IS" BASIS,
       | * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
       | * See the License for the specific language governing permissions and
       | * limitations under the License.
       | */""".stripMargin
}

object TypeGenerationConfig {

  // TODO it would be nice to use absolute paths, but this presently
  // causes problems in test compilation
  val DefaultImports: String =
    """|import {TreeNode,FormatInfo,PathExpressionEngine} from '../tree/PathExpression'
       |import {ProjectContext} from '../operations/ProjectEditor'
       |""".stripMargin

  val TestStubImports: String =
      """
        |import {GraphNode} from '../../tree/PathExpression'
      """.stripMargin
}