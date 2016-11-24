package com.atomist.rug.rugdoc

import _root_.java.nio.charset.Charset
import _root_.java.util.Objects

import com.atomist.param.Parameter
import com.atomist.project.ProjectOperationArguments
import com.atomist.project.common.InvalidParametersException
import com.atomist.project.common.support.ProjectOperationParameterSupport
import com.atomist.project.edit._
import com.atomist.project.generate.ProjectGenerator
import com.atomist.rug.kind.DefaultTypeRegistry
import com.atomist.rug.kind.java.support.JavaHelpers
import com.atomist.rug.spi.{StaticTypeInformation, Type, TypeRegistry}
import com.atomist.rug.ts.TypeScriptGenerationHelper
import com.atomist.source.{ArtifactSource, FileArtifact, SimpleFileBasedArtifactSource, StringFileArtifact}
import com.atomist.util.template.velocity.VelocityMergeTool
import org.apache.commons.io.IOUtils

import scala.collection.mutable.ListBuffer


object TypeScriptInterfaceGenerator {

  val DefaultTemplateName = "ts.vm"

  val DefaultDocName = "rugKinds.md"

  val OutputPathParam = "output_path"

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

  // TODO don't use a template except for the header. Use String operations

  import TypeScriptInterfaceGenerator._

  addParameter(Parameter(OutputPathParam, ".*").
    setRequired(false).
    setDisplayName("Path for created doc").
    setDefaultValue(DefaultDocName))

  @throws[InvalidParametersException](classOf[InvalidParametersException])
  override def generate(poa: ProjectOperationArguments): ArtifactSource = {
    val createdFile = emitInterfaces(poa)
    val output = StringFileArtifact(DefaultDocName, createdFile.content)
    new SimpleFileBasedArtifactSource("RugDocs", output)
  }

  private def generateType(t: Type): String = {
    val output = new StringBuilder("")
    val tsName = helper.typeScriptClassNameForTypeName(t.name)
    output.++=(
      s"""
         |/*
         | * ${t.description}
         | */""".stripMargin)
    output.++=(s"\ninterface $tsName {\n\n")
    t.typeInformation match {
      case s: StaticTypeInformation =>
        for {
          op <- s.operations
          if !op.parameters.exists(_.parameterType.contains("FunctionInvocationContext"))
        } {
          val comment =
            for (p <- op.parameters)
              yield s"$indent//${p.name}: ${helper.javaTypeToTypeScriptType(p.parameterType)}"
          val params =
            for (p <- op.parameters)
              yield s"${p.name}: ${helper.javaTypeToTypeScriptType(p.parameterType)}"
          output ++= s"${comment.mkString("\n")}\n$indent${op.name}(${params.mkString(", ")}): ${helper.javaTypeToTypeScriptType(op.returnType)}\n\n"
        }
    }
    output ++= s"}${indent.dropRight(1)}// interface $tsName"
    output.toString
  }


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
    val output: StringBuilder = new StringBuilder(
      """
        |/*
        |* Licensed under the Ap
        |*/
        |
      """.stripMargin
    )

    def generate(t: Type): Unit = {
      output ++= s"${generateType(t)}\n\n"
      alreadyGenerated.append(t)
    }

    for {
      t <- typeRegistry.kinds
      if !alreadyGenerated.contains(t)
    } {
      var clazzAncestry: Seq[Type] =
        Seq(t)

      // TODO DON'T IMPLEMENT SUBCLASS METHOD TWICE and put in inheritance
      val parentType: Option[Type] =
        Option(t.underlyingType.getSuperclass)
          .flatMap(sup => {
            println(s"Checking superclass $sup of ${t.underlyingType}")
            typeRegistry.kinds.find(_.underlyingType.equals(sup))
          })

      clazzAncestry = clazzAncestry ++ parentType

      clazzAncestry.reverse.foreach(t => generate(t))
    }

    StringFileArtifact(
      Objects.toString(poa.parameterValues.find(p => p.getName.equals(OutputPathParam)).getOrElse(DefaultDocName)),
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
