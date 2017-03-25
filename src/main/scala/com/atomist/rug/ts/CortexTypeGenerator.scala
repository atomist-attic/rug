package com.atomist.rug.ts

import com.atomist.param.SimpleParameterValues
import com.atomist.rug.spi.{SimpleTypeRegistry, TypeOperation, TypeRegistry, Typed}
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
      Prop(propName, genTyp)
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
        val props = n("properties") match {
          case l: List[List[String]]@unchecked if l.head.isInstanceOf[Seq[_]] =>
            l.map(l => defineProperty(l.head, l(1)))
          case l: List[String]@unchecked =>
            Seq(defineProperty(l.head, l(1)))
        }

        val pn = PropertyNode(
          n("labels").asInstanceOf[List[String]].toSet,
          props
        )
        pn
      })

    // These are normally a - [right,cardinality] - right
    // [["Org"], ["HAS", "1:M", ["OWNED_BY", "1:1"]], ["Repo"]]
    val allRelationships: Traversable[Relationship] =
    doc("relationships") flatMap {
      case List(left: String, List(name: String, card: String), right: String) =>
        val cardinality = Cardinality(card)
        val relName = toTypeScriptIdentifier(name)
        Seq(Relationship(left, relName, cardinality, right))
      case List(left: String, List(name: String, card: String, List(backName: String, backCard: String)), right: String) =>
        val cardinality = Cardinality(card)
        val relName = toTypeScriptIdentifier(name)
        val backCardinality = Cardinality(backCard)
        val backRelName = toTypeScriptIdentifier(backName)
        val backRel = Relationship(right, backRelName, backCardinality, left)
        //println(s"bidirectional relationship $backRel")
        Seq(
          Relationship(left, relName, cardinality, right),
          backRel
        )
      case x =>
        throw new IllegalArgumentException(s"Illegal list, not able to destructure, $x")
    }

    propertyNodes.map(pn => new JsonBackedTyped(pn, allRelationships)).toSet
  }

  private def toTypeScriptIdentifier(name: String): String =
    toCamelizedPropertyName(name.toLowerCase)
}

private case class Prop(name: String, typ: String)

private case class PropertyNode(labels: Set[String], properties: Seq[Prop])

private sealed trait Cardinality

private object Cardinality {

  def apply(s: String): Cardinality = s match {
    case "1:1" => OneToOne
    case "1:M" => OneToM
    case x => throw new IllegalArgumentException(s"Unknown cardinality: [$x]")
  }
}

private case object OneToOne extends Cardinality

private case object OneToM extends Cardinality

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
            case OneToOne => rel.right
            case OneToM => s"${rel.right}[]"
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
            description = prop.typ,
            readOnly = false,
            parameters = Nil,
            returnType = prop.typ,
            definedOn = null,
            example = None
          ))

    propsOps ++ relOps
  }

  override def toString: String =
    s"$name: ops=[${operations.mkString(",")}]"
}
