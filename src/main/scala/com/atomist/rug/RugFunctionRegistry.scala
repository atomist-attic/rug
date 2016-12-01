package com.atomist.rug

import com.atomist.rug.runtime.rugdsl.{NoOpFunction, RugFunction}

trait RugFunctionRegistry {

  def findByName[T,R](name: String): Option[RugFunction[T,R]]

  def defined(name: String) = findByName(name).isDefined

  def functions: Set[RugFunction[_,_]]

  def plus(that: RugFunctionRegistry): RugFunctionRegistry = {
    def toMap(rfr: RugFunctionRegistry): Map[String, RugFunction[_,_]] = {
      val m = new scala.collection.mutable.HashMap[String, RugFunction[_,_]]
      rfr.functions.foreach(f => m.put(f.name, f))
      m.toMap
    }
    val combined = new FixedRugFunctionRegistry(toMap(this) ++ toMap(that))
    combined
  }

  def +(that: RugFunctionRegistry) = this plus that
}

case class FixedRugFunctionRegistry(
                                     pm: Map[String, RugFunction[_,_]] = Map())
  extends RugFunctionRegistry {

  private val m = pm + (
    "eval" -> NoOpFunction
    )

  def this(functions: Traversable[RugFunction[_,_]]) =
    this(
      functions.map(f => (f.name, f)).toMap[String,RugFunction[_,_]]
    )

  override def functions: Set[RugFunction[_, _]] = m.values.toSet

  override def findByName[T,R](name: String): Option[RugFunction[T,R]] =
    m.get(name).asInstanceOf[Option[RugFunction[T,R]]]
}

object FixedRugFunctionRegistry {

  val Empty = FixedRugFunctionRegistry()

  import scala.collection.JavaConverters._

  def fromJavaMap(jm: java.util.Map[String, RugFunction[_,_]]) = {
    FixedRugFunctionRegistry(jm.asScala.toMap)
  }
}

/**
  * For use in Spring context as default
  */
class EmptyRugFunctionRegistry extends FixedRugFunctionRegistry()