package com.atomist.rug.ts

import java.util.Objects

import com.atomist.param.SimpleParameterValues
import com.atomist.rug.spi.{SimpleTypeRegistry, TypeOperation, Typed}
import com.atomist.source.{ArtifactSource, StringFileArtifact}
import com.atomist.util.lang.JavaHelpers
import com.atomist.util.misc.ResourceLoading
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import com.atomist.util.misc.ResourceLoading._

/**
  * Take endpoints reported in JSON from a materializer service and generate types
  * from which we can in turn generate TypeScript interfaces.
  * There's little error checking, as this isn't intended as production code.
  */
class TypeGenerator {

  private val mapper = new ObjectMapper() with ScalaObjectMapper
  mapper.registerModule(DefaultScalaModule)

  private def toTypeScriptFiles(json: String): ArtifactSource = {
    val types = extract(json)
    val tig = new TypeScriptInterfaceGenerator(typeRegistry =
      new SimpleTypeRegistry(types))
    tig.generate("types", SimpleParameterValues("output_path", "Types.ts"))
  }

  /**
    * Return a valid node module with these files in it
    * @return
    */
  def toNodeModule(json: String): ArtifactSource = {
    toTypeScriptFiles(json).withPathAbove("model") +
      resourcesInPackage(classToPath(this) + "/gen")
  }

  /**
    * Extract the types described in this document and output a valid node module
    */
  def extract(json: String): Set[Typed] = {
    val doc: Map[String, List[_]] = mapper.readValue(json, classOf[Map[String, List[_]]])

    val propertyNodes: Traversable[PropertyNode] =
      doc("nodes").map(node => {
        val n = node.asInstanceOf[Map[String, _]]
        val props = n("properties") match {
          case l: List[List[String]]@unchecked if l.head.isInstanceOf[Seq[_]] =>
            l.map(l => Prop(l(0), l(1)))
          case l: List[String]@unchecked =>
            Seq(Prop(l(0), l(1)))
        }

        val pn = PropertyNode(
          n("labels").asInstanceOf[List[String]].toSet,
          props
        )
        pn
      })

    // These are normally a - b - b
    val relationships: Traversable[Relationship] =
      doc("relationships") flatMap {
        case relList: List[_]@unchecked if relList.size == 3 =>

          val relName = toTypeScriptName(relList(1) match {
            case s: String => s
            case l: List[_] => Objects.toString(l.head)
          })
          val a = relList(0) match {
            case s: String => List(s)
            case l: List[_] => l.map(Objects.toString)
          }
          val b = relList(2) match {
            case s: String => List(s)
            case l: List[_] => l.map(Objects.toString)
          }
          val r = Relationship(a.toSet, relName, b.toSet)
          Seq(r)
        case _ => ???
      }

    propertyNodes.map(pn => new JsonBackedTyped(pn,
      relationships.filter(_.as.contains(pn.labels.head)))
    ).toSet
  }

  private def toTypeScriptName(name: String): String =
    JavaHelpers.toCamelizedPropertyName(name.toLowerCase)

}

private case class Prop(name: String, typ: String)

private case class PropertyNode(labels: Set[String], properties: Seq[Prop])

private case class Relationship(as: Set[String], rel: String, bs: Set[String])

// TODO do we need to have multiple labels in Typed?

private class JsonBackedTyped(
                               properties: PropertyNode,
                               relationships: Traversable[Relationship]
                             ) extends Typed {

  override val name: String = properties.labels.head

  override def description: String = s"Type $name"

  // TODO should come from parent?
  override def allOperations: Seq[TypeOperation] = operations

  override def operations: Seq[TypeOperation] = {
    val propsOps: Seq[TypeOperation] =
      properties.properties.map(prop => TypeOperation(
        name = prop.name,
        description = prop.typ,
        readOnly = false,
        parameters = Nil,
        returnType = "String",
        definedOn = null,
        example = None
      ))
    val relOps: Seq[TypeOperation] =
      relationships.map(rel => TypeOperation(
        name = rel.rel,
        description = s"$name - ${rel.as.head} -> ${rel.bs}",
        readOnly = false,
        parameters = Nil,
        returnType = rel.bs.head,
        definedOn = null,
        example = None
      )).toSeq

    propsOps ++ relOps
  }

  override def toString: String =
    s"$name: ops=[${operations.mkString(",")}]"

}

object TypeGenerator {

  val PackageJson =
    """
      |{
      |  "dependencies": {
      |    "@atomist/rug": "0.12.0"
      |  }
      |}
      |
    """.stripMargin
}
