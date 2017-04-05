package com.atomist.rug.ts

import com.atomist.param.SimpleParameterValues
import com.atomist.rug.spi._
import com.atomist.source.ArtifactSource
import com.atomist.util.lang.JavaHelpers._
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper

object CortexTypeGenerator {

  val DefaultCortexDir = "cortex"

  val DefaultCortexStubDir = "cortex/stub"

  def extendedTypes(cortexJson: String): TypeRegistry = {
    val types = new CortexTypeGenerator(DefaultCortexDir, DefaultCortexDir).extract(cortexJson)
    new SimpleTypeRegistry(types)
  }

}

/**
  * Take endpoints reported in JSON from a materializer service and generate types
  */
class CortexTypeGenerator(basePackage: String, baseClassPackage: String) {

  private val mapper = new ObjectMapper() with ScalaObjectMapper
  mapper.registerModule(DefaultScalaModule)

  private def toTypeScriptFiles(json: String): ArtifactSource = {
    val types = extract(json)
    val typeRegistry = new SimpleTypeRegistry(types)
    //types.foreach(println(_))
    val tig = new TypeScriptInterfaceGenerator(typeRegistry, root = "GraphNode")
    val tcg = new TypeScriptStubClassGenerator(typeRegistry)
    tig.generate("types", SimpleParameterValues("output_path", "Types.ts")).withPathAbove(basePackage) +
      tcg.generate("types", SimpleParameterValues("output_path", "Types.ts")).withPathAbove(baseClassPackage)
  }

  /**
    * Return a valid node module with these files in it, under base package
    */
  def toNodeModule(json: String): ArtifactSource = {
    toTypeScriptFiles(json)
  }

  /**
    * Extract the types described in this document and output a valid node module
    */
  def extract(json: String): Set[Typed] = {
    require(json != null)
    val doc: Map[String, List[_]] = mapper.readValue(json, classOf[Map[String, List[_]]])

    def defineProperty(rawName: String, typ: String): Prop = {
      // Do aliasing
      val propName = toTypeScriptIdentifier(rawName)
      val genTyp = typ match {
        case "string" => "String"
        case "number" => "long"
        case "url" => "String" // TODO we should somehow use helpers for this and construct in the proxy from a string
        case "timestamp" => "String"
        case x => x
      }
      SimpleProp(propName, genTyp)
    }

    /*
    {"nodes":
    {"labels":["Push"],
    "properties":
    [["before", "string"], ["after", "string"],
    ["timestamp", "timestamp"], ["branch", "string"]],
    "unique":["branch", "after"]},
     */
    val propertyNodes: Traversable[PropertyNode] =
      doc("nodes").map(node => {
        // A node is of form labels, properties, unique?
        val n = node.asInstanceOf[Map[String, _]]
        val props: Seq[Prop] = n("properties") match {
          case l: List[_] =>
            l.map {
              case List(id: String, typ: String) =>
                defineProperty(id, typ)
              case List(id: String, legalValues: List[String]@unchecked) =>
                defineProperty(id, "String")
              // TODO we can't yet support enums in the generation process
              // so we return string for now
              //EnumProp(id, legalValues)
            }
        }

        val pn = PropertyNode(
          n("labels").asInstanceOf[List[String]].toSet,
          props
        )
        pn
      })

    // These are normally a - [right,cardinality] - right
    // ["Org", ["HAS", ["repo", "1:M"], ["org", "1:1"]], "Repo"]
    // List(ChatTeam, List(OWNS, List(orgs, 1:M), List(chatTeam, 1:1)), Org)
    val allRelationships: Traversable[Relationship] =
    doc.getOrElse("relationships", Nil) flatMap {
      case List(leftEntity: String, List(_: String, List(lrName: String, lrCard: String), List(rlName: String, rlCard: String)), rightEntity: String) =>
        Seq(
          Relationship(leftEntity, lrName, Cardinality(lrCard), rightEntity),
          Relationship(rightEntity, rlName, Cardinality(rlCard), leftEntity)
        )
      case List(leftEntity: String, List(discard: String, List(lrName: String, lrCard: String)), rightEntity: String) =>
        Seq(
          Relationship(leftEntity, lrName, Cardinality(lrCard), rightEntity)
        )
      case x =>
        throw new IllegalArgumentException(s"Illegal list, not able to destructure, $x")
    }

    propertyNodes.map(pn => new JsonBackedTyped(pn, allRelationships)).toSet
  }

  private def toTypeScriptIdentifier(name: String): String =
    toCamelizedPropertyName(name.toLowerCase)
}

private trait Prop {

  def name: String

  def description: String = name

  def toType: ParameterOrReturnType
}

private case class SimpleProp(name: String, typ: String) extends Prop {

  def toType: ParameterOrReturnType = SimpleParameterOrReturnType(typ)
}

private case class EnumProp(name: String, legalValues: Seq[String]) extends Prop {

  def toType: ParameterOrReturnType = EnumParameterOrReturnType(name, legalValues)
}

private case class PropertyNode(labels: Set[String], properties: Seq[Prop])

private case class Relationship(left: String, name: String, cardinality: Cardinality, right: String)

private class JsonBackedTyped(
                               properties: PropertyNode,
                               allRelationships: Traversable[Relationship]
                             ) extends Typed {

  override val name: String = properties.labels.head

  override def description: String = s"Type $name"

  // TODO should come from parent?
  override def allOperations: Seq[TypeOperation] = operations

  override def operations: Seq[TypeOperation] = {
    val relOps: Seq[TypeOperation] =
      allRelationships
        .filter(_.left == name)
        .map(rel => TypeOperation(
          name = rel.name,
          description = s"${rel.name} - ${rel.left} -> ${rel.right}",
          readOnly = false,
          parameters = Nil,
          returnType = rel.cardinality match {
            case OneToOne => SimpleParameterOrReturnType(rel.right)
            case OneToM => SimpleParameterOrReturnType(rel.right, isArray = true)
          },
          definedOn = null,
          example = None
        )).toSeq
    val propsOps: Seq[TypeOperation] =
      properties.properties
        // Filter out properties that have the same name as a relationship, that takes precedence
        .filterNot(p => relOps.exists(_.name == p.name))
        .map(prop =>
          TypeOperation(
            name = prop.name,
            description = prop.description,
            readOnly = false,
            parameters = Nil,
            returnType = prop.toType,
            definedOn = null,
            example = None
          ))

    propsOps ++ relOps
  }

  override def toString: String =
    s"$name: ops=[${operations.mkString(",")}]"
}
