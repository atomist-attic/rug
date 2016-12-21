package com.atomist.rug.spi

import java.util.{List => JList}

import com.atomist.rug.RugRuntimeException
import com.atomist.rug.ts.NashornUtils

/**
  * Type information about a language element such as a Type.
  * Useful for tooling and document generation as well as
  * compile time validation.
  */
sealed trait TypeInformation

/**
  * TypeInformation subtrait indicating when operations
  * on the type are not known.
  */
trait DynamicTypeInformation extends TypeInformation

/**
  * Trait that types should extend when all operations on the type are known.
  * ReflectiveStaticTypeInformation subinterface computes this automatically
  * using reflection.
  *
  * @see ReflectiveStaticTypeInformation
  */
trait StaticTypeInformation extends TypeInformation {

  def operations: Seq[TypeOperation]

}

/**
  * Parameter to a Rug type.
  */
case class TypeParameter(
                          name: String,
                          parameterType: String,
                          description: Option[String]
                        ) {

  def getDescription: String = description.getOrElse("")

  override def toString: String = {
    name + " : " + parameterType + " : " + description.getOrElse("No Description")
  }
}

/**
  * Operation on an exported Rug type. Typically annotated with an
  * [[ExportFunction]] annotation
  *
  * @param name        name of the type
  * @param description description of the type. May be used in generated code
  * @param example     optional example of usage of the operation
  * @see ExportFunction
  */
case class TypeOperation(
                          name: String,
                          description: String,
                          readOnly: Boolean,
                          parameters: Seq[TypeParameter],
                          returnType: String,
                          example: Option[String]) {

  def hasExample: Boolean = example.isDefined

  /**
    * Convenient way of invoking the method using arguments
    * passed directly from Javascript.
    */
  def invoke(target: Object, rawArgs: Seq[AnyRef]): Object = {
    val args = rawArgs.map(a => NashornUtils.toJavaType(a))
    val methods = target.getClass.getMethods.toSeq.filter(m =>
      this.name.equals(m.getName) &&
        m.getDeclaredAnnotations.exists(ann => ann.isInstanceOf[ExportFunction]) &&
        this.parameters.size == m.getParameterCount
    )
    if (methods.size != 1)
      throw new IllegalArgumentException(
        s"Operation [$name] cannot be invoked on [${target.getClass.getName}]: Found ${methods.size} definitions with ${parameters.size}, required exactly 1: " +
          methods.mkString(","))
    //println(s"About to invoke ${methods.head} with args=$args")
    try {
      methods.head.invoke(target, args: _*)
    }
    catch {
      case t: Throwable =>
        val argDiagnostics = args map {
          case null => "null"
          case o => s"$o: ${o.getClass}"
        }
        throw new RugRuntimeException(null, s"Exception invoking ${methods.head} with args=${argDiagnostics.mkString(",")}: ${t.getMessage}", t)
    }
  }
}
