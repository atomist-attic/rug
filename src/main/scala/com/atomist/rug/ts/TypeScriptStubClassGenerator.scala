package com.atomist.rug.ts

import com.atomist.param.Tag
import com.atomist.rug.spi._
import com.atomist.util.lang.JavaHelpers._

import scala.collection.mutable.ListBuffer

private object TypeScriptStubClassGenerator {

  val DefaultTypedocBoilerplate: String =
    """
      |Generated class exposing Atomist Cortex.
      |Fluent builder style class for use in testing and query by example.""".stripMargin

  val GraphNodeMethodImplementationDoc: String =
    "Implementation of GraphNode interface method.\nFor infrastructure, not user use"
}

/**
  * Generate stub classes for testing.
  *
  * @param typeRegistry registry of known Rug Types.
  */
class TypeScriptStubClassGenerator(typeRegistry: TypeRegistry,
                                   config: TypeGenerationConfig = TypeGenerationConfig(imports = TypeGenerationConfig.TestStubImports),
                                   override val tags: Seq[Tag] = Nil,
                                   val root: String = "GraphNode")
  extends AbstractTypeScriptGenerator(typeRegistry, config, true, tags) {

  import TypeScriptStubClassGenerator._

  private case class ClassGeneratedType(name: String,
                                        description: String,
                                        methods: Seq[MethodInfo],
                                        parent: Seq[String] = Seq(root))
    extends GeneratedType {

    private val interfaceModuleImport = "api"

    override def root: String = TypeScriptStubClassGenerator.this.root

    override def specificImports: String =
      s"""import * as $interfaceModuleImport from "../$name";\n"""

    override def toString: String = {
      val output = new StringBuilder
      output ++= helper.toJsDoc(description + "\n" + DefaultTypedocBoilerplate)
      output ++= s"\nclass $name "
      val implementation = s"implements $interfaceModuleImport.$name"
      output ++= (if (parent.head == root) {
        implementation
      }
      else
        s"extends ${parent.mkString(", ")} $implementation"
        )
      output ++= s" {${config.separator}"

      // Output private variables
      output ++= methods
        .map(mi =>
          s"${indent}private ${toFieldName(mi)}: ${mi.returnType};")
        .mkString("\n")
      output ++= config.separator

      // Emit methods from GraphNode We need a tag of "-dynamic" to allow dispatch in the proxy
      output ++=
        helper.indented(
          s"""|${helper.toJsDoc(GraphNodeMethodImplementationDoc)}
              |nodeName(): string {
              |${indent}return "$name";
              |}
              |
              |${helper.toJsDoc(GraphNodeMethodImplementationDoc)}
              |nodeTags(): string[] {
              |${indent}return [ "$name", "-dynamic" ];
              |}""".stripMargin, 1)
      output ++= config.separator
      output ++= methods.map(_.toString).mkString(config.separator)
      output ++= s"${if (methods.isEmpty) "" else config.separator}}${indent.dropRight(1)}"
      output.toString
    }
  }

  private def toFieldName(m: MethodInfo): String = "_" + m.name

  private case class ClassMethodInfo(typeName: String,
                                     name: String,
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
          val core =
            s"""|$comment$indent$name(${params.mkString(", ")}): $returnType {
                |$indent${indent}return this.${toFieldName(this)};
                |$indent}""".stripMargin
          val builderMethod =
            if (returnsArray) {
              // It's an array type. Create an "addX" method to add the value to the array,
              // initializing it if necessary
              s"""
                 |
                 |${indent}add${upperize(name)}($name: ${underlyingType}): $typeName {
                 |$indent${indent}if (this.${toFieldName(this)} === undefined)
                 |$indent$indent${indent}this.${toFieldName(this)} = [];
                 |$indent${indent}this.${toFieldName(this)}.push($name);
                 |$indent${indent}return this;
                 |$indent}""".stripMargin
            }
            else {
              // It's a scalar. Create a "withX" method to set the value
              s"""
                 |
                 |${indent}with${upperize(name)}($name: $returnType): $typeName {
                 |$indent${indent}this.${toFieldName(this)} = $name;
                 |$indent${indent}return this;
                 |$indent}""".stripMargin
            }
          core + builderMethod
      }

  }

  override protected def getMethodInfo(typeName: String, op: TypeOperation, params: Seq[MethodParam]): MethodInfo =
    ClassMethodInfo(typeName, op.name, params, helper.javaTypeToTypeScriptType(op.returnType, typeRegistry), Some(op.description))

  override def getGeneratedTypes(t: Typed, op: TypeOperation): Seq[GeneratedType] = {
    val generatedTypes = new ListBuffer[GeneratedType]
    val alreadyAddedMethods = new ListBuffer[MethodInfo]

    // Get super classes
    val superClasses = getSuperClasses(op)
    for (i <- superClasses.indices) {
      val name = Typed.typeToTypeName(superClasses(i).parent)
      val parent = if (i == superClasses.size - 1) Seq(root) else Seq(Typed.typeToTypeName(superClasses(i + 1).parent))
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

    leafClassMethods ++= allMethods(t.name, t.operations).filterNot(alreadyAddedMethods.contains(_))
    val parent = if (superClasses.isEmpty) Seq(root) else Seq(Typed.typeToTypeName(superClasses.head.parent))
    generatedTypes += ClassGeneratedType(t.name, t.description, leafClassMethods, parent)

    generatedTypes
  }
}
