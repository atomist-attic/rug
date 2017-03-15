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
import com.atomist.util.lang.{JavaHelpers, TypeScriptGenerationHelper}
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
                                           config: InterfaceGenerationConfig,
                                           generateClasses: Boolean,
                                           override val tags: Seq[Tag])
  extends ProjectGenerator
    with ProjectEditor
    with RugSupport {

  import AbstractTypeScriptGenerator._

  val outputPathParam = "output_path"

  private val helper = new TypeScriptGenerationHelper()
  private val typeSort: (Typed, Typed) => Boolean = (a, b) => a.name <= b.name
  private val indent = "    "

  protected case class ParentClassHolder(parent: Class[_], exportedMethods: Seq[MethodInfo])

  /**
    * Either an interface or a test class, depending on whether we're generated test classes
    */
  protected case class GeneratedType(name: String, description: String, methods: Seq[MethodInfo], parent: Seq[String] = Seq(Root)) {

    override def toString: String = {
      val output = new StringBuilder
      output ++= emitDocComment(description)
      if (generateClasses) {
        val deriveKeyword = if (parent.head == Root) "implements" else "extends"
        output ++= s"\nclass $name $deriveKeyword ${parent.mkString(", ")} {${config.separator}"
      } else {
        if (parent.isEmpty)
          output ++= s"\ninterface $name {${config.separator}"
        else
          output ++= s"\ninterface $name extends ${parent.mkString(", ")} {${config.separator}"
      }

      output ++= methods.map(_.toString).mkString(config.separator)
      output ++= s"${if (methods.isEmpty) "" else config.separator}}${indent.dropRight(1)}"
      output.toString
    }
  }

  protected class MethodInfo(val name: String, val params: Seq[MethodParam], val returnType: String, val description: Option[String]) {

    private val comment = {
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

    def canEqual(a: Any) = a.isInstanceOf[MethodInfo]

    override def equals(that: Any) =
      that match {
        case that: MethodInfo => that.canEqual(this) && this.name.hashCode == that.name.hashCode &&
          this.params.hashCode == that.params.hashCode && this.returnType.hashCode == that.returnType.hashCode
        case _ => false
      }

    override def hashCode() =
      Seq(name, params, returnType).map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)

    override def toString =
      if (generateClasses) returnType match {
        case "void" =>
          s"""$comment$indent$name(${params.mkString(", ")}): $returnType {}
         """.stripMargin
        case _ =>
          // It has a return. So let's create a field
          val fieldName = s"_$name"
          // TODO shouldn't be any in the setter:
          // Need to pass in owning type
          s"""${indent}private $fieldName: $returnType = null
             |
             |${indent}with${JavaHelpers.upperize(name)}(x: $returnType): any {
             |$indent${indent}this.$fieldName = x
             |$indent${indent}return this
             |$indent}
             |
             |$comment$indent$name(${params.mkString(", ")}): $returnType {
             |$indent${indent}return this.$fieldName
             |$indent}""".stripMargin
      } else
        s"$comment$indent$name(${params.mkString(", ")}): $returnType"
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

  protected def getSuperInterfaces(op: TypeOperation) = {
    if (op.definedOn == null) Nil
    else
      ClassUtils.getAllInterfaces(op.definedOn).asScala
        .filterNot(c => classOf[TreeNode] == c || classOf[GraphNode] == c)
        .map(c => ParentClassHolder(c, allMethods(exportedOperations(c))))
        .filterNot(_.exportedMethods.isEmpty)
        .toList
  }

  protected def getSuperClasses(op: TypeOperation) = {
    if (op.definedOn == null) Nil
    else
      ClassUtils.getAllSuperclasses(op.definedOn).asScala
        .filterNot(c => classOf[TreeNode] == c || classOf[GraphNode] == c)
        .map(c => ParentClassHolder(c, allMethods(exportedOperations(c))))
        .filterNot(_.exportedMethods.isEmpty)
        .toList
  }

  protected def allMethods(ops: Seq[TypeOperation]): Seq[MethodInfo] = {
    val methods = new ListBuffer[MethodInfo]
    for {
      op <- ops
      if shouldEmit(op)
    } {
      val params =
        for (p <- op.parameters)
          yield
            MethodParam(p.name, helper.javaTypeToTypeScriptType(p.parameterType, typeRegistry), p.description)

      methods += new MethodInfo(op.name, params, helper.javaTypeToTypeScriptType(op.returnType, typeRegistry), Some(op.description))
    }
    methods.sortBy(_.name)
  }

  private def shouldEmit(top: TypeOperation) =
    !(top.parameters.exists(_.parameterType.contains("FunctionInvocationContext")) || "eval".equals(top.name))

  private def emitDocComment(description: String): String = {
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

  protected def allGeneratedTypes(types: Seq[Typed]): Seq[GeneratedType]

  private def getImports(interfaceTypes: Seq[GeneratedType], currentType: GeneratedType) = {
    val imports = interfaceTypes
      .flatMap(t => currentType.methods.map(m => StringUtils.removeEnd(m.returnType, "[]")).filter(currentType.name != t.name && _ == t.name))
      .toList
    (currentType.parent.toList ::: imports).distinct.filter(_ != Root).map(i => s"""import {$i} from "./$i"""").mkString("\n")
  }

  override def applicability(as: ArtifactSource): Applicability = Applicability.OK

  override def description: String = "Generate core Rug type info"

  override def name: String = "TypedDoc"
}

case class InterfaceGenerationConfig(
                                      indent: String = "    ",
                                      separator: String = "\n\n"
                                    )
  extends TypeScriptGenerationConfig {

  val imports: String =
    """|import {TreeNode,FormatInfo,PathExpressionEngine} from '../tree/PathExpression'
       |import {ProjectContext} from '../operations/ProjectEditor'
       |""".stripMargin

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
