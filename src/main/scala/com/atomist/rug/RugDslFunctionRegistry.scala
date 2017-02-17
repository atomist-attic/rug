package com.atomist.rug

import com.atomist.rug.runtime.rugdsl.{NoOpDslFunction, RugDslFunction}

trait RugDslFunctionRegistry {

  def findByName[T,R](name: String): Option[RugDslFunction[T,R]]

  def defined(name: String) = findByName(name).isDefined

  def functions: Set[RugDslFunction[_,_]]

  def plus(that: RugDslFunctionRegistry): RugDslFunctionRegistry = {
    def toMap(rfr: RugDslFunctionRegistry): Map[String, RugDslFunction[_,_]] = {
      val m = new scala.collection.mutable.HashMap[String, RugDslFunction[_,_]]
      rfr.functions.foreach(f => m.put(f.name, f))
      m.toMap
    }
    val combined = new FixedRugDslFunctionRegistry(toMap(this) ++ toMap(that))
    combined
  }

  def +(that: RugDslFunctionRegistry) = this plus that
}

case class FixedRugDslFunctionRegistry(
                                     pm: Map[String, RugDslFunction[_,_]] = Map())
  extends RugDslFunctionRegistry {

  private val m = pm + (
    "eval" -> NoOpDslFunction
    )

  def this(functions: Traversable[RugDslFunction[_,_]]) =
    this(
      functions.map(f => (f.name, f)).toMap[String,RugDslFunction[_,_]]
    )

  override def functions: Set[RugDslFunction[_, _]] = m.values.toSet

  override def findByName[T,R](name: String): Option[RugDslFunction[T,R]] =
    m.get(name).asInstanceOf[Option[RugDslFunction[T,R]]]
}

object FixedRugDslFunctionRegistry {

  val Empty = FixedRugDslFunctionRegistry()

  import scala.collection.JavaConverters._

  def fromJavaMap(jm: java.util.Map[String, RugDslFunction[_,_]]) = {
    FixedRugDslFunctionRegistry(jm.asScala.toMap)
  }
}

/**
  * For use in Spring context as default
  */
class EmptyRugDslFunctionRegistry extends FixedRugDslFunctionRegistry()
