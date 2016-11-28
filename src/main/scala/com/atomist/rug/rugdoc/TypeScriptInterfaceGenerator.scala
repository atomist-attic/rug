package com.atomist.rug.rugdoc

import com.atomist.param.Parameter
import com.atomist.project.ProjectOperationArguments
import com.atomist.project.common.InvalidParametersException
import com.atomist.project.common.support.ProjectOperationParameterSupport
import com.atomist.project.edit._
import com.atomist.project.generate.ProjectGenerator
import com.atomist.rug.kind.DefaultTypeRegistry
import com.atomist.rug.spi._
import com.atomist.rug.ts.TypeScriptGenerationHelper
import com.atomist.source.{ArtifactSource, FileArtifact, SimpleFileBasedArtifactSource, StringFileArtifact}

import scala.collection.mutable.ListBuffer

object TypeScriptInterfaceGenerator {

  val DefaultTemplateName = "ts.vm"

  val DefaultFilename = "model/Core.ts"

  val OutputPathParam = "output_path"

  val LicenseHeader =
    """
      |/*
      | * Copyright 2015-2016 Atomist Inc.
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
      | */
    """.stripMargin

}

/**
  * Generate types for documents
  *
  * @param typeRegistry Registry of known Rug Types.
  */
class TypeScriptInterfaceGenerator(
                                    typeRegistry: TypeRegistry = DefaultTypeRegistry
                                  ) extends ProjectGenerator
  with ProjectEditor with ProjectOperationParameterSupport {

  val helper = new TypeScriptGenerationHelper()

  import TypeScriptInterfaceGenerator._

  addParameter(Parameter(OutputPathParam, ".*").
    setRequired(false).
    setDisplayName("Path for created doc").
    setDefaultValue(DefaultFilename))

  @throws[InvalidParametersException](classOf[InvalidParametersException])
  override def generate(poa: ProjectOperationArguments): ArtifactSource = {
    val createdFile = emitInterfaces(poa)
    new SimpleFileBasedArtifactSource("Rug user model", createdFile)
  }

  private def shouldEmit(top: TypeOperation) =
    !(top.parameters.exists(_.parameterType.contains("FunctionInvocationContext")) ||
      "eval".equals(top.name)
      )

  private def emitDocComment(t: Type): String = {
    s"""
       |/*
       | * ${t.description}
       | */""".stripMargin
  }

  private def emitDocComment(top: TypeOperation): String = {
    (for (p <- top.parameters)
      yield s"$indent//${p.name}: ${helper.javaTypeToTypeScriptType(p.parameterType)}")
      .mkString("\n")
  }

  private def emitParameter(p: TypeParameter): String = {
    s"${p.name}: ${helper.javaTypeToTypeScriptType(p.parameterType)}"
  }

  private def generateType(t: Type): String = {
    val output = new StringBuilder("")
    val tsName = helper.typeScriptClassNameForTypeName(t.name)
    output ++= emitDocComment(t)
    output.++=(s"\ninterface $tsName {\n\n")
    t.typeInformation match {
      case s: StaticTypeInformation =>
        for {
          op <- s.operations
          if shouldEmit(op)
        } {
          val comment = emitDocComment(op)
          val params =
            for (p <- op.parameters)
              yield emitParameter(p)
          output ++= s"$comment\n$indent${op.name}(${params.mkString(", ")}): ${helper.javaTypeToTypeScriptType(op.returnType)}\n\n"
        }
    }
    output ++= s"}${indent.dropRight(1)}// interface $tsName"
    output.toString
  }

  val typeSort: (Type, Type) => Boolean = (a, b) => a.name <= b.name

  private def emitInterfaces(poa: ProjectOperationArguments): FileArtifact = {
    //    val template = IOUtils.toString(getClass.getResourceAsStream("/" + DefaultTemplateName), Charset.defaultCharset())
    //    val templates = new SimpleFileBasedArtifactSource("template", StringFileArtifact(DefaultTemplateName, template))
    //    val mt = new VelocityMergeTool(templates)
    //    println(s"Generating for ${
    //      typeRegistry.kinds.size
    //    } kinds")

    val alreadyGenerated = ListBuffer.empty[Type]

    //    val f = mt.mergeToFile(MergeContext(
    //      Map(
    //        "kinds" -> typeRegistry.kinds.asJava,
    //        "h2" -> "##",
    //        "h3" -> "###",
    //        "h4" -> "####"
    //      )
    //    ), DefaultTemplateName)
    // TODO could be a template
    val output: StringBuilder = new StringBuilder(LicenseHeader)

    def generate(t: Type): Unit = {
      output ++= s"${generateType(t)}\n\n"
      alreadyGenerated.append(t)
    }

    for {
      t <- typeRegistry.kinds.sortWith(typeSort)
      if !alreadyGenerated.contains(t)
    } {
      var clazzAncestry: Seq[Type] =
        Seq(t)

      // TODO DON'T IMPLEMENT SUBCLASS METHOD TWICE and put in inheritance
      val parentType: Option[Type] =
      Option(t.underlyingType.getSuperclass)
        .flatMap(sup => typeRegistry.kinds.find(_.underlyingType.equals(sup)))

      clazzAncestry = clazzAncestry ++ parentType

      clazzAncestry.reverse.foreach(t => generate(t))
    }

    output ++= "\n"
    for {t <- typeRegistry.kinds.sortWith(typeSort)} {
      val tsName = helper.typeScriptClassNameForTypeName(t.name)
      output ++= s"export { $tsName }\n"
    }

    StringFileArtifact(
      poa.stringParamValue(OutputPathParam),
      output.toString())
  }

  private val indent = "    "

  override def modify(as: ArtifactSource, poa: ProjectOperationArguments): ModificationAttempt = {
    val createdFile = emitInterfaces(poa)
    val r = as + createdFile
    SuccessfulModification(r, impacts, "OK")
  }

  override def impacts: Set[Impact] = Set(ReadmeImpact)

  override def applicability(as: ArtifactSource): Applicability = Applicability.OK

  override def description: String = "Generate core Rug type info"

  override def name: String = "TypeDoc"

}
