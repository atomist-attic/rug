package com.atomist.rug.ts

import java.io.PrintWriter

import com.atomist.param.Parameter
import com.atomist.project.common.InvalidParametersException
import com.atomist.project.common.support.ProjectOperationParameterSupport
import com.atomist.project.edit._
import com.atomist.project.generate.ProjectGenerator
import com.atomist.project.{ProjectOperationArguments, SimpleProjectOperationArguments}
import com.atomist.rug.kind.DefaultTypeRegistry
import com.atomist.rug.spi._
import com.atomist.source.{ArtifactSource, FileArtifact, SimpleFileBasedArtifactSource, StringFileArtifact}
import com.atomist.util.Utils
import com.atomist.util.lang.TypeScriptGenerationHelper
import org.apache.commons.lang3.StringUtils

import scala.collection.mutable.ListBuffer

object TypeScriptInterfaceGenerator extends App {

  val target = if (args.length < 1) "target/Core.ts" else args.head

  val generator = new TypeScriptInterfaceGenerator

  val output = generator.generate("", SimpleProjectOperationArguments("", Map(generator.OutputPathParam -> "Core.ts")))
  Utils.withCloseable(new PrintWriter(target))(_.write(output.allFiles.head.content))
  println(s"Written to $target")
}

/**
  * Generate types for documents.
  *
  * @param typeRegistry registry of known Rug Types.
  */
class TypeScriptInterfaceGenerator(typeRegistry: TypeRegistry = DefaultTypeRegistry,
                                   config: InterfaceGenerationConfig = InterfaceGenerationConfig())
  extends ProjectGenerator
    with ProjectEditor
    with ProjectOperationParameterSupport {

  val DefaultTemplateName = "ts.vm"

  val DefaultFilename = "model/Core.ts"

  val OutputPathParam = "output_path"

  private val helper = new TypeScriptGenerationHelper()

  private val root = "TreeNode"

  private val typeSort: (Typed, Typed) => Boolean = (a, b) => a.name <= b.name

  private val indent = "    "

  private case class InterfaceType(name: String, description: String, methods: Seq[MethodInfo], parent: String = root) {

    override def toString: String = {
      val output = new StringBuilder("")
      output ++= emitDocComment(description)
      output ++= s"\ninterface $name extends $parent {${config.separator}"
      output ++= methods.map(_.toString).mkString(config.separator)
      output ++= s"${if (methods.isEmpty) "" else config.separator}}${indent.dropRight(1)}"
      output.toString
    }
  }

  private case class MethodInfo(name: String, params: Seq[MethodParam], returnType: String) {

    private val comment =
      if (params.isEmpty)
        ""
      else
        (for (p <- params)
          yield s"$indent//$p")
          .mkString("\n") + "\n"

    override def toString = s"$comment$indent$name(${params.mkString(", ")}): $returnType"
  }

  private class MethodParam(val name: String, val paramType: String) {

    def canEqual(a: Any): Boolean = a.isInstanceOf[MethodParam]

    override def equals(that: Any): Boolean =
      that match {
        case that: MethodParam => that.canEqual(this) && this.paramType.hashCode == that.paramType.hashCode
        case _ => false
      }

    override def hashCode: Int = paramType.hashCode

    override def toString = s"$name: $paramType"
  }

  private object MethodParam {

    def apply(name: String, paramType: String) = new MethodParam(name, paramType)
  }

  addParameter(Parameter(OutputPathParam, ".*")
    .setRequired(false)
    .setDisplayName("Path for created doc")
    .setDefaultValue(DefaultFilename))

  @throws[InvalidParametersException](classOf[InvalidParametersException])
  override def generate(projectName: String, poa: ProjectOperationArguments): ArtifactSource = {
    val createdFiles = emitInterfaces(poa)
    new SimpleFileBasedArtifactSource("Rug user model", createdFiles)
  }

  private def shouldEmit(top: TypeOperation) =
    !(top.parameters.exists(_.parameterType.contains("FunctionInvocationContext")) || "eval".equals(top.name))

  private def emitDocComment(description: String): String = {
    s"""
       |/*
       | * $description
       | */""".stripMargin
  }

  private def allInterfaceTypes(allTypes: Seq[Typed]): Seq[InterfaceType] =
    allTypes.map(t => InterfaceType(t.name, t.description, allMethods(t)))

  private def allMethods(t: Typed): Seq[MethodInfo] =
    t.typeInformation match {
      case st: StaticTypeInformation => allMethods(st.operations)
      case _ => Nil
    }

  private def allMethods(ops: Seq[TypeOperation]): Seq[MethodInfo] = {
    val methods = new ListBuffer[MethodInfo]
    for {
      op <- ops
      if shouldEmit(op)
    } {
      val params =
        for (p <- op.parameters)
          yield
            MethodParam(p.name, helper.javaTypeToTypeScriptType(p.parameterType))

      methods += MethodInfo(op.name, params, helper.javaTypeToTypeScriptType(op.returnType))
    }
    methods
  }

  private def emitInterfaces(poa: ProjectOperationArguments): Seq[FileArtifact] = {
    val alreadyGenerated = ListBuffer.empty[InterfaceType]
    val tsInterfaces = ListBuffer.empty[StringFileArtifact]

    val allTypes = typeRegistry.types.sortWith(typeSort)
    val interfaceTypes = allInterfaceTypes(allTypes)
    val pathParam = poa.stringParamValue(OutputPathParam)

    for {
      t <- interfaceTypes
      if !alreadyGenerated.contains(t)
    } {
      val t1 = t
      val output = new StringBuilder(config.licenseHeader)
      output ++= config.separator
      output ++= config.imports
      output ++= "\n"

      val returnTypes = new ListBuffer[String]
      interfaceTypes.foreach(t => t1.methods.foreach(m => {
        val returnType = StringUtils.removeEnd(m.returnType, "[]")
        if (!t1.name.equals(t.name) && returnType.equals(t.name))
          returnTypes += returnType
      }))
      returnTypes.distinct.foreach(rt => {
        output ++= s"""import {$rt} from "./$rt""""
        output ++= "\n"
      })

      output ++= config.separator
      output ++= t.toString
      output ++= config.separator
      output ++= s"export {${t.name}}\n"

      val path = if (pathParam.contains("/")) StringUtils.substringBeforeLast(pathParam, "/") + "/" else ""
      tsInterfaces += StringFileArtifact(s"$path${t.name}.ts", output.toString())
      alreadyGenerated.append(t)
    }

    val output = new StringBuilder(config.licenseHeader)
    output ++= config.separator
    output ++= config.separator

    alreadyGenerated.foreach(t => {
      output ++= s"""import {${t.name}} from "./${t.name}""""
      output ++= "\n"
      output ++= s"export {${t.name}}\n"
    })

    tsInterfaces += StringFileArtifact(pathParam, output.toString())
    tsInterfaces
  }

  override def modify(as: ArtifactSource, poa: ProjectOperationArguments): ModificationAttempt = {
    val createdFile = emitInterfaces(poa)
    val r = as + createdFile
    SuccessfulModification(r)
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
    """
      |import {TreeNode,FormatInfo,PathExpressionEngine} from '../tree/PathExpression'
      |import {ProjectContext} from '../operations/ProjectEditor' """
      .stripMargin

  val licenseHeader: String =
    """
      |/*
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
