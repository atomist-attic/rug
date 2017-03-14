package com.atomist.rug.ts

import java.io.PrintWriter

import com.atomist.graph.GraphNode
import com.atomist.param.{SimpleParameterValues, Tag}
import com.atomist.rug.kind.DefaultTypeRegistry
import com.atomist.rug.spi.ReflectiveFunctionExport.exportedOperations
import com.atomist.rug.spi._
import com.atomist.tree.TreeNode
import com.atomist.util.Utils
import org.apache.commons.lang3.ClassUtils.{getAllInterfaces, getAllSuperclasses}

import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer

object TypeScriptInterfaceGenerator extends App {

  val target = if (args.length < 1) "target/Core.ts" else args.head
  val generator = new TypeScriptInterfaceGenerator
  val output = generator.generate("", SimpleParameterValues(Map(generator.OutputPathParam -> target)))
  output.allFiles.foreach(f => Utils.withCloseable(new PrintWriter(f.path))(_.write(f.content)))
}

/**
  * Generate interfacestypes for documents.
  *
  * @param typeRegistry registry of known Rug Types.
  */
class TypeScriptInterfaceGenerator(typeRegistry: TypeRegistry = DefaultTypeRegistry,
                                   config: InterfaceGenerationConfig = InterfaceGenerationConfig(),
                                   override val tags: Seq[Tag] = Nil)
  extends AbstractTypeScriptGenerator(typeRegistry, config, false, tags) {

  protected def allGeneratedTypes(allTypes: Seq[Typed]): Seq[GeneratedType] = {
    val generatedTypes = new ListBuffer[GeneratedType]
    allTypes.foreach(t => {
      t.operations.foreach(op => {
        // Add super classes
        val superClasses =
          if (op.definedOn == null) Nil
          else
            getAllSuperclasses(op.definedOn).asScala
              .filterNot(c => allMethods(exportedOperations(c)).isEmpty)
              .filterNot(c => classOf[TreeNode] == c || classOf[GraphNode] == c)
              .toList

        val alreadyAddedMethods = new ListBuffer[MethodInfo]
        for (i <- superClasses.indices) {
          val name = Typed.typeToTypeName(superClasses(i))
          val ops = exportedOperations(superClasses(i))
          val methods = allMethods(ops)
          val parent = if (i == superClasses.size - 1) Seq(root) else Seq(Typed.typeToTypeName(superClasses(i + 1)))
          generatedTypes += GeneratedType(name, description, methods, parent)
          alreadyAddedMethods ++= methods
        }

        // Add super interfaces
        val superInterfaces =
          if (op.definedOn == null) Nil
          else
            getAllInterfaces(op.definedOn).asScala
              .filterNot(c => allMethods(exportedOperations(c)).isEmpty)
              .filterNot(c => classOf[TreeNode] == c || classOf[GraphNode] == c)
              .toList

        for (i <- superInterfaces.size to 1 by -1) {
          val ops = exportedOperations(superInterfaces(i - 1))
          val name = Typed.typeToTypeName(superInterfaces(i - 1))
          val methods = allMethods(ops)
          generatedTypes += GeneratedType(name, name, methods, Seq())
          alreadyAddedMethods ++= methods
        }

        // Add leaf class
        val parent = if (superClasses.isEmpty && superInterfaces.isEmpty) Seq(root) else Seq(Typed.typeToTypeName(superClasses.head)) ++ superInterfaces.map(i => Typed.typeToTypeName(i))
        val methods = allMethods(t.operations).filterNot(alreadyAddedMethods.contains(_))
        generatedTypes += GeneratedType(t.name, t.description, methods, parent)
      })
    })

    (generatedTypes.groupBy(_.name) map {
      case (_, l) => l.head
    }).toSeq.sortBy(_.name)
  }
}
