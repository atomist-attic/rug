package com.atomist.rug.rugdoc

import _root_.java.nio.charset.Charset
import _root_.java.util

import com.atomist.param.Parameter
import com.atomist.project.ProjectOperationArguments
import com.atomist.project.common.InvalidParametersException
import com.atomist.project.common.support.ProjectOperationParameterSupport
import com.atomist.project.common.template.{MergeContext, VelocityMergeTool}
import com.atomist.project.edit._
import com.atomist.project.generate.ProjectGenerator
import com.atomist.rug.kind.DefaultTypeRegistry
import com.atomist.rug.spi.{StaticTypeInformation, TypeOperation, TypeRegistry}
import com.atomist.source.{ArtifactSource, FileArtifact, SimpleFileBasedArtifactSource, StringFileArtifact}
import org.apache.commons.io.IOUtils

import collection.JavaConverters._

object TypeDoc {

  val DefaultTemplateName = "kind.vm"

  val DefaultDocName = "rugKinds.md"

  val OutputPathParam = "output_path"
}

/**
  * Generate types for documents
  *
  * @param typeRegistry Registry of known Rug Types.
  */
class TypeDoc(
               typeRegistry: TypeRegistry = DefaultTypeRegistry
             ) extends ProjectGenerator
  with ProjectEditor with ProjectOperationParameterSupport {

  import TypeDoc._

  addParameter(Parameter(OutputPathParam, ".*").
    setRequired(false).
    setDisplayName("Path for created doc").
    setDefaultValue(DefaultDocName))

  @throws[InvalidParametersException](classOf[InvalidParametersException])
  override def generate(poa: ProjectOperationArguments): ArtifactSource = {
    val createdFile = createFile(poa)
    val output = StringFileArtifact(DefaultDocName, createdFile.content)
    new SimpleFileBasedArtifactSource("RugDocs", output)
  }

  private def createFile(poa: ProjectOperationArguments): FileArtifact = {
    val template = IOUtils.toString(getClass.getResourceAsStream("/" + DefaultTemplateName), Charset.defaultCharset())
    val templates = new SimpleFileBasedArtifactSource("template", StringFileArtifact(DefaultTemplateName, template))
    val mt = new VelocityMergeTool(templates)
    val kindsInfo: util.List[util.Map[String,Object]] = typesToContext
    val f = mt.mergeToFile(MergeContext(
      Map(
        "kinds" -> kindsInfo,
        "h2" -> "##",
        "h3" -> "###",
        "h4" -> "####",
        "h5" -> "#####"
      )
    ), DefaultTemplateName)
    StringFileArtifact(
      util.Objects.toString(poa.parameterValues.find(p => p.getName.equals(OutputPathParam)).getOrElse(DefaultDocName)),
      f.content)
  }

  private def typesToContext: util.List[util.Map[String,Object]] = {
    typeRegistry.types map { t =>
      val operations: Seq[TypeOperation] = t.typeInformation match {
        case o: StaticTypeInformation => o.operations
        case _ => Seq.empty
      }
      val ops: Seq[util.Map[String, Object]] = operations map { o =>
        val params: Seq[util.Map[String, String]] = o.parameters map { p =>
          Map(
            "name" -> p.name,
            "parameterType" -> p.parameterType,
            "description" -> p.getDescription
          ).asJava
        }
        Map(
          "name" -> o.name,
          "description" -> o.description,
          "parameters" -> params.asJava
        ).asJava
      }
      Map(
        "name" -> t.name,
        "description" -> t.description,
        "operations" -> ops.asJava
      ).asJava
    }
  }.asJava

  override def modify(as: ArtifactSource, poa: ProjectOperationArguments): ModificationAttempt = {
    val createdFile = createFile(poa)
    val r = as + createdFile
    SuccessfulModification(r, "OK")
  }

  override def applicability(as: ArtifactSource): Applicability = Applicability.OK

  override def description: String = "Generate core Rug type info"

  override def name: String = "TypeDoc"
}
