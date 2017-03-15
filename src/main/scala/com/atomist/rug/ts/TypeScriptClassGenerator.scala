package com.atomist.rug.ts

import java.io.PrintWriter

import com.atomist.param.{SimpleParameterValues, Tag}
import com.atomist.rug.kind.DefaultTypeRegistry
import com.atomist.rug.spi._
import com.atomist.util.Utils
import com.atomist.util.lang.JavaHelpers

import scala.collection.mutable.ListBuffer

object TypeScriptClassGenerator extends App {

  val target = if (args.length < 1) "target/Core.ts" else args.head
  val generator = new TypeScriptClassGenerator
  val output = generator.generate("", SimpleParameterValues(Map(generator.outputPathParam -> target)))
  output.allFiles.foreach(f => Utils.withCloseable(new PrintWriter(f.path))(_.write(f.content)))
}

/**
  * Generate stub classes for testing.
  *
  * @param typeRegistry registry of known Rug Types.
  */
class TypeScriptClassGenerator(typeRegistry: TypeRegistry = DefaultTypeRegistry,
                               config: InterfaceGenerationConfig = InterfaceGenerationConfig(),
                               override val tags: Seq[Tag] = Nil)
  extends AbstractTypeScriptGenerator(typeRegistry, config, true, tags) {

  import AbstractTypeScriptGenerator._

  private case class ClassGeneratedType(name: String,
                                        description: String,
                                        methods: Seq[MethodInfo],
                                        parent: Seq[String] = Seq(Root))
    extends GeneratedType {

    override def toString: String = {
      val output = new StringBuilder
      output ++= emitDocComment(description)
      val deriveKeyword = if (parent.head == Root) "implements" else "extends"
      output ++= s"\nclass $name $deriveKeyword ${parent.mkString(", ")} {${config.separator}"
      output ++= methods.map(_.toString).mkString(config.separator)
      output ++= s"${if (methods.isEmpty) "" else config.separator}}${indent.dropRight(1)}"
      output.toString
    }
  }

  private case class ClassMethodInfo(name: String,
                                     params: Seq[MethodParam],
                                     returnType: String,
                                     description: Option[String])
    extends MethodInfo {

    override def toString: String =
      returnType match {
        case "void" =>
          s"""$comment$indent$name(${params.mkString(", ")}): $returnType {}
             """.stripMargin
        case _ =>
          // It has a return. So let's create a field
          val fieldName = s"_$name"
          // TODO shouldn't be any in the setter:
          // Need to pass in owning type
          s"""${indent}private $fieldName: $returnType = null
             |
             |${indent}with${JavaHelpers.upperize(name)}(x: $returnType): any {
             |$indent${indent}this.$fieldName = x
             |$indent${indent}return this
             |$indent}
             |
             |$comment$indent$name(${params.mkString(", ")}): $returnType {
             |$indent${indent}return this.$fieldName
             |$indent}""".stripMargin
      }
  }

  override protected def getMethodInfo(op: TypeOperation, params: Seq[MethodParam]): MethodInfo =
    ClassMethodInfo(op.name, params, helper.javaTypeToTypeScriptType(op.returnType, typeRegistry), Some(op.description))

  override def getGeneratedTypes(t: Typed, op: TypeOperation): Seq[GeneratedType] = {
    val generatedTypes = new ListBuffer[GeneratedType]
    val alreadyAddedMethods = new ListBuffer[MethodInfo]

    // Get super classes
    val superClasses = getSuperClasses(op)
    for (i <- superClasses.indices) {
      val name = Typed.typeToTypeName(superClasses(i).parent)
      val parent = if (i == superClasses.size - 1) Seq(Root) else Seq(Typed.typeToTypeName(superClasses(i + 1).parent))
      val methods = superClasses(i).exportedMethods.filterNot(alreadyAddedMethods.contains(_))
      generatedTypes += ClassGeneratedType(name, name, methods, parent)
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
    generatedTypes += ClassGeneratedType(t.name, t.description, leafClassMethods, parent)

    generatedTypes
  }
}
