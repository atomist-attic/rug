package com.atomist.rug.spi

import java.util.{List => JList}

import com.atomist.rug.RugRuntimeException
import com.atomist.rug.ts.NashornUtils
import com.typesafe.scalalogging.LazyLogging

import scala.collection.JavaConverters._

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
  * @see ReflectiveStaticTypeInformation
  */
trait StaticTypeInformation extends TypeInformation {

  def operations: Seq[TypeOperation]

  /**
    * Exposes for callers who may need Java
    * @return
    */
  def operationsAsJava: JList[TypeOperation] = operations.asJava

}

case class TypeParameter(
                   name: String,
                   parameterType: String,
                   description: Option[String]
                   ) {

  def getDescription: String = description.getOrElse("")

  override def toString: String =
    name + " : " + parameterType + " : " + description.getOrElse("No Description")
}

// TODO flesh out parameters to include type information
case class TypeOperation(
                          name: String,
                          description: String,
                          readOnly: Boolean,
                          parameters: Seq[TypeParameter],
                          returnType: String,
                          example: Option[String])
  extends LazyLogging {

  def parametersAsJava: JList[TypeParameter] = parameters.asJava

  def hasExample = example.isDefined

  def exampleAsJava = example.getOrElse("")

  def invoke(target: Object, rawArgs: Seq[AnyRef]): Object = {
    val args = rawArgs.map(a => NashornUtils.toJavaType(a))
    val methods = target.getClass.getMethods.toSeq.filter(m =>
      this.name.equals(m.getName) && this.parameters.size == m.getParameterCount
    )
    if (methods.size != 1)
      throw new IllegalArgumentException(s"Operation [$name] cannot be invoked on [${target.getClass.getName}]: Found ${methods.size} definitions with ${parameters.size}, required exactly 1")

    target match {
      case mv: MutableView[_] => logger.debug(s"Target parent=${mv.parent}")
      case _ =>
    }

    try {
      methods.head.invoke(target, args:_*)
    } catch {
      case t: Throwable =>
        val argDiagnostics = args map {
          case null => "null"
          case o => s"$o: ${o.getClass}"
        }
        throw new RugRuntimeException(null, s"Exception invoking ${methods.head} with args=${argDiagnostics.mkString(",")}: ${t.getMessage}", t)
    }
  }
}
