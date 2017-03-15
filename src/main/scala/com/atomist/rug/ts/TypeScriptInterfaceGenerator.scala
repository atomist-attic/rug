package com.atomist.rug.ts

import java.io.PrintWriter

import com.atomist.param.{SimpleParameterValues, Tag}
import com.atomist.rug.kind.DefaultTypeRegistry
import com.atomist.rug.spi._
import com.atomist.util.Utils

import scala.collection.mutable.ListBuffer

object TypeScriptInterfaceGenerator extends App {

  val target = if (args.length < 1) "target/Core.ts" else args.head
  val generator = new TypeScriptInterfaceGenerator
  val output = generator.generate("", SimpleParameterValues(Map(generator.outputPathParam -> target)))
  output.allFiles.foreach(f => Utils.withCloseable(new PrintWriter(f.path))(_.write(f.content)))
}

/**
  * Generate interface types for documents.
  *
  * @param typeRegistry registry of known Rug Types.
  */
class TypeScriptInterfaceGenerator(typeRegistry: TypeRegistry = DefaultTypeRegistry,
                                   config: InterfaceGenerationConfig = InterfaceGenerationConfig(),
                                   override val tags: Seq[Tag] = Nil)
  extends AbstractTypeScriptGenerator(typeRegistry, config, false, tags) {

  import AbstractTypeScriptGenerator._

  override def getGeneratedTypes(t: Typed, op: TypeOperation): Seq[GeneratedType] = {
    val generatedTypes = new ListBuffer[GeneratedType]
    val alreadyAddedMethods = new ListBuffer[MethodInfo]

    // Get super classes
    val superClasses = getSuperClasses(op)
    for (i <- superClasses.indices) {
      val name = Typed.typeToTypeName(superClasses(i).parent)
      val parent = if (i == superClasses.size - 1) Seq(Root) else Seq(Typed.typeToTypeName(superClasses(i + 1).parent))
      val methods = superClasses(i).exportedMethods
      generatedTypes += GeneratedType(name, description, methods, parent)
      alreadyAddedMethods ++= methods
    }

    // Get super interfaces
    val superInterfaces = getSuperInterfaces(op)
    for (i <- superInterfaces.size to 1 by -1) {
      val name = Typed.typeToTypeName(superInterfaces(i - 1).parent)
      val methods = superInterfaces(i - 1).exportedMethods
      generatedTypes += GeneratedType(name, name, methods, Seq())
      alreadyAddedMethods ++= methods
    }

    // Add leaf class
    val parent =
      if (superClasses.isEmpty && superInterfaces.isEmpty) Seq(Root)
      else Seq(Typed.typeToTypeName(superClasses.head.parent)) ++ superInterfaces.map(i => Typed.typeToTypeName(i.parent))

    val methods = allMethods(t.operations).filterNot(alreadyAddedMethods.contains(_))
    generatedTypes += GeneratedType(t.name, t.description, methods, parent)

    generatedTypes
  }
}
