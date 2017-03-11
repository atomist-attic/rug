package com.atomist.rug.ts

import com.atomist.rug.spi.{TypeOperation, Typed}
import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper

// TODO idea of also generating classes: In Interface generator


/**
  * Take endpoints reported in JSON from a materializer service and generate types
  * from which we can in turn generate TypeScript interfaces.
  */
class TypeGenerator {

  private val mapper = new ObjectMapper() with ScalaObjectMapper
  mapper.registerModule(DefaultScalaModule)

  /**
    * Extract the types described in this document
    */
  def extract(json: String): Set[Typed] = {
    val doc: Map[String, List[_]] = mapper.readValue(json, classOf[Map[String, List[_]]])

    val propertyNodes: Traversable[PropertyNode] =
      doc("nodes").map(node => {
        //println(s"Found node $node")
        val n = node.asInstanceOf[Map[String, _]]
        val props = n("properties") match {
          case l : List[List[String]]@unchecked if l.head.isInstanceOf[Seq[_]] =>
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

    val relationships: Traversable[Relationship] =
      doc("relationships").map(rel => {
        val relList = rel.asInstanceOf[List[List[String]]].flatten
        val r = Relationship(relList(0), relList(1), relList(2))
        //println(s"Found relationship $r")
        r
      })

    propertyNodes.map(pn => new SimpleTyped(pn,
      relationships.filter(_.a == pn.labels.head))
    ).toSet
  }

}

private case class Prop(name: String, typ: String)

private case class PropertyNode(labels: Set[String], properties: Seq[Prop])

private case class Relationship(a: String, rel: String, b: String)

// TODO do we need to have multiple labels in Typed?

private class SimpleTyped(
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
        description = s"$name - ${rel.rel} -> ${rel.b}",
        readOnly = false,
        parameters = Nil,
        returnType = rel.b,
        definedOn = null,
        example = None
      )).toSeq

    propsOps ++ relOps
  }

  override def toString: String =
    s"$name: ops=[${operations.mkString(",")}]"

}
