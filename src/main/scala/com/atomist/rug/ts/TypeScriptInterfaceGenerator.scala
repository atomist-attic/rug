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
                                   config: TypeGenerationConfig = TypeGenerationConfig(),
                                   override val tags: Seq[Tag] = Nil,
                                   root: String = "TreeNode")
  extends AbstractTypeScriptGenerator(typeRegistry, config, tags) {

  private case class InterfaceGeneratedType(name: String,
                                            description: String,
                                            methods: Seq[MethodInfo],
                                            parent: Seq[String] = Seq(root))
    extends GeneratedType {

    override def root: String = TypeScriptInterfaceGenerator.this.root

    override def toString: String = {
      val output = new StringBuilder
      output ++= helper.toJsDoc(description)
      if (parent.isEmpty)
        output ++= s"\ninterface $name {${config.separator}"
      else
        output ++= s"\ninterface $name extends ${parent.mkString(", ")} {${config.separator}"

      output ++= methods.map(_.toString).mkString(config.separator)
      output ++= s"${if (methods.isEmpty) "" else config.separator}}${indent.dropRight(1)}"
      output.toString
    }
  }

  private case class InterfaceMethodInfo(typeName: String,
                                         name: String,
                                         params: Seq[MethodParam],
                                         returnType: String,
                                         description: Option[String],
                                         exposeAsProperty: Boolean)
    extends MethodInfo {

    override def toString: String = {
      if (exposeAsProperty) {
        // Emit property only
        s"$comment${indent}readonly $name: $returnType;"
      }
      else {
        // Emit function
        s"$comment$indent$name(${params.mkString(", ")}): $returnType;"
      }
    }
  }

  protected def getMethodInfo(typeName: String, op: TypeOperation, params: Seq[MethodParam]): MethodInfo =
    InterfaceMethodInfo(typeName, op.name, params, helper.rugTypeToTypeScriptType(op.returnType, typeRegistry),
      Some(op.description), op.exposeAsProperty)

  override def getGeneratedTypes(t: Typed, op: TypeOperation): Seq[GeneratedType] = {
    val generatedTypes = new ListBuffer[GeneratedType]
    val alreadyAddedMethods = new ListBuffer[MethodInfo]

    // Get super classes
    val superClasses = getSuperClasses(op)
    for (i <- superClasses.indices) {
      val name = Typed.typeToTypeName(superClasses(i).parent)
      val parent = if (i == superClasses.size - 1) Seq(root) else Seq(Typed.typeToTypeName(superClasses(i + 1).parent))
      val methods = superClasses(i).exportedMethods
      generatedTypes += InterfaceGeneratedType(name, name, methods, parent)
      alreadyAddedMethods ++= methods
    }

    // Get super interfaces
    val superInterfaces = getSuperInterfaces(op)
    for (i <- superInterfaces.size to 1 by -1) {
      val name = Typed.typeToTypeName(superInterfaces(i - 1).parent)
      val methods = superInterfaces(i - 1).exportedMethods
      generatedTypes += InterfaceGeneratedType(name, name, methods, Seq())
      alreadyAddedMethods ++= methods
    }

    // Add leaf class
    val parent =
      if (superClasses.isEmpty && superInterfaces.isEmpty) Seq(root)
      else Seq(Typed.typeToTypeName(superClasses.head.parent)) ++ superInterfaces.map(i => Typed.typeToTypeName(i.parent))

    val methods = allMethods(t.name, t.operations).filterNot(alreadyAddedMethods.contains(_))
    generatedTypes += InterfaceGeneratedType(t.name, t.description, methods, parent)

    generatedTypes
  }
}
