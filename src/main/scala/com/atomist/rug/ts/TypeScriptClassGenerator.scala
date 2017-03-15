package com.atomist.rug.ts

import java.io.PrintWriter

import com.atomist.param.{SimpleParameterValues, Tag}
import com.atomist.rug.kind.DefaultTypeRegistry
import com.atomist.rug.spi._
import com.atomist.util.Utils

import scala.collection.mutable.ListBuffer

object TypeScriptClassGenerator extends App {

  val target = if (args.length < 1) "target/Core.ts" else args.head
  val generator = new TypeScriptClassGenerator
  val output = generator.generate("", SimpleParameterValues(Map(generator.outputPathParam -> target)))
  output.allFiles.foreach(f => Utils.withCloseable(new PrintWriter(f.path))(_.write(f.content)))
}

/**
  * Generate stub class for testing.
  *
  * @param typeRegistry registry of known Rug Types.
  */
class TypeScriptClassGenerator(typeRegistry: TypeRegistry = DefaultTypeRegistry,
                               config: InterfaceGenerationConfig = InterfaceGenerationConfig(),
                               override val tags: Seq[Tag] = Nil)
  extends AbstractTypeScriptGenerator(typeRegistry, config, true, tags) {

  import AbstractTypeScriptGenerator._

  protected def allGeneratedTypes(allTypes: Seq[Typed]): Seq[GeneratedType] = {
    val generatedTypes = new ListBuffer[GeneratedType]
    allTypes.foreach(t => {
      t.operations.foreach(op => {
        val alreadyAddedMethods = new ListBuffer[MethodInfo]

        // Get super classes
        val superClasses = getSuperClasses(op)
        for (i <- superClasses.indices) {
          val name = Typed.typeToTypeName(superClasses(i).parent)
          val parent = if (i == superClasses.size - 1) Seq(Root) else Seq(Typed.typeToTypeName(superClasses(i + 1).parent))
          val methods = superClasses(i).exportedMethods.filterNot(alreadyAddedMethods.contains(_))
          generatedTypes += GeneratedType(name, description, methods, parent)
          alreadyAddedMethods ++= methods
        }

        // Get super interfaces
        val superInterfaces = getSuperInterfaces(op)

        // Add leaf class
        val leafClassMethods = new ListBuffer[MethodInfo]
        for (i <- superInterfaces.size to 1 by -1) {
          val methods = superInterfaces(i - 1).exportedMethods.filterNot(alreadyAddedMethods.contains(_))
          leafClassMethods ++= methods
          alreadyAddedMethods ++= methods
        }

        leafClassMethods ++= allMethods(t.operations).filterNot(alreadyAddedMethods.contains(_))
        val parent = if (superClasses.isEmpty) Seq(Root) else Seq(Typed.typeToTypeName(superClasses.head.parent))
        generatedTypes += GeneratedType(t.name, t.description, leafClassMethods, parent)
      })
    })

    (generatedTypes.groupBy(_.name) map {
      case (_, l) => l.head
    }).toSeq.sortBy(_.name)
  }
}
