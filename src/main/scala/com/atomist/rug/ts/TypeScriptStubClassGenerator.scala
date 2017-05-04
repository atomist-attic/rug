package com.atomist.rug.ts

import com.atomist.param.Tag
import com.atomist.rug.spi._
import com.atomist.util.lang.JavaHelpers._
import org.apache.commons.lang3.StringUtils

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
  extends AbstractTypeScriptGenerator(typeRegistry, config, tags) {

  import TypeScriptStubClassGenerator._

  private case class ClassGeneratedType(name: String,
                                        description: String,
                                        methods: Seq[MethodInfo],
                                        parent: Seq[String] = Seq(root))
    extends GeneratedType {

    private val interfaceModuleImport = s"${StringUtils.uncapitalize(name)}Api"

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
        .map(mi => {
          val visibility = "private"
          s"$indent$visibility ${toFieldName(mi)}: ${mi.returnType};"
        })
        .mkString("\n")
      output ++= config.separator

      output ++= graphNodeImpl(name)
      output ++= config.separator
      output ++= methods.map(_.toString).mkString(config.separator)
      output ++= s"${if (methods.isEmpty) "" else config.separator}}${indent.dropRight(1)}"
      output.toString
    }
  }

  // Implementation of fields and methods from GraphNode interface
  private def graphNodeImpl(name: String): String = {
    // Create fields to make JSON stringification more revealing
    helper.indented(
      s"""|nodeName: string = "$name";
          |
          |nodeTags: string[] = [ "$name", "-dynamic" ];
          |
          |""".stripMargin, 1)
  }

  private def toFieldName(m: MethodInfo): String = "_" + m.name

  private case class ClassMethodInfo(typeName: String,
                                     name: String,
                                     params: Seq[MethodParam],
                                     returnType: String,
                                     description: Option[String],
                                     exposeAsProperty: Boolean)
    extends MethodInfo {

    override def toString: String = {
      returnType match {
        case "void" =>
          s"""${comment("")}$indent$name(${params.mkString(", ")}): $returnType {}
             """.stripMargin
        case _ =>
          // It has a return. So let's create a field
          val fieldName = toFieldName(this)

          val core = if (exposeAsProperty) {
            helper.indented(
              s"""${comment("")}get $name(): $returnType {
              |${indent}if (this.$fieldName === undefined) {
              |$indent${indent}throw new Error(`Please use the relevant builder method to set property [$name] on stub [$typeName] before accessing it. It's probably called [with${upperize(name)}]`)
              |$indent}
              |${indent}return this.$fieldName;
              |}
            """.stripMargin, 1)
          }
          else {
            s"""|${comment("")}$indent$name(${params.mkString(", ")}): $returnType {
                |$indent${indent}if (this.$fieldName} === undefined)
                |$indent$indent${indent}throw new Error(`Please use the relevant builder method to set property [$name] on stub [$typeName] before accessing it. It's probably called [with${upperize(name)}]`)
                |$indent$indent${indent}return this.${toFieldName(this)};
                |$indent}""".stripMargin
          }
          val builderMethod =
            // Rely on type inference in return types, as this increases
            // flexibility for proxying etc.
            if (returnsArray) {
              // It's an array type. Create an "addX" method to add the value to the array,
              // initializing it if necessary
              helper.indented(
                s"""|
                    |${helper.toJsDoc(s"Fluent builder method to add an element to the $name array")}
                    |add${upperize(name)}(...$name: $underlyingType[]) {
                    |${indent}if (this.$fieldName === undefined)
                    |$indent${indent}this.$fieldName = [];
                    |${indent}this.$fieldName = this.$fieldName.concat($name);
                    |${indent}return this;
                    |}""".stripMargin, 1)
            }
            else {
              // It's a scalar. Create a "withX" method to set the value
              helper.indented(
                s"""|
                    |${helper.toJsDoc(s"Fluent builder method to set the $name property")}
                    |with${upperize(name)}($name: $returnType) {
                    |${indent}this.$fieldName = $name;
                    |${indent}return this;
                    |}""".stripMargin, 1)
            }
          core + builderMethod
      }
    }
  }

  override protected def getMethodInfo(typeName: String, op: TypeOperation, params: Seq[MethodParam]): MethodInfo =
    ClassMethodInfo(typeName, op.name, params,
      helper.rugTypeToTypeScriptType(op.returnType, typeRegistry),
      Some(op.description),
      op.exposeAsProperty)

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
