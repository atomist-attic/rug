package com.atomist.project.common.support

import _root_.java.lang.reflect.Method
import _root_.java.util.{List => JList}

import com.atomist.param.{ParameterValue, ParameterValues, Parameterized, SimpleParameterValue}
import com.typesafe.scalalogging.LazyLogging

import scala.collection.JavaConversions._

/**
  * Override computedParameters to reflectively invoke methods
  * Parameter computation methods must be annotated with @ComputedParameter
  * and must be public, taking a single argument of type ProjectDeltaInfo
  */
// TODO could pull up to parameter-lib as it's generic parameter functioanlity
class ReflectiveParameterComputer(target: Any) extends ParameterComputer with LazyLogging {

  private val namesAndComputedParameterMethods: Seq[(String, Method)] =
    for {
      m <- target.getClass.getMethods
      a = m.getAnnotation[ComputedParameter](classOf[ComputedParameter])
      if a != null
      x = logger.debug(s"Found computed parameter method $m")
    }
    // TODO check that the method takes only one argument, of type ProjectOperationInfo
      yield (a.value, {
        m.setAccessible(true)
        m
      })

  logger.info(s"Analyzing ${getClass.getName}: Found computed parameters methods: ${namesAndComputedParameterMethods.mkString(",")}")

  private def methodsToParameters(pvs: ParameterValues): List[ParameterValue] =
    namesAndComputedParameterMethods.flatMap(tup => {
      val name = tup._1
      logger.debug(s"About to invoke ${tup._2} to compute parameter name $name")
      try {
        val result = tup._2.invoke(target, pvs).toString
        val detyped: ParameterValue = SimpleParameterValue(name, result)
        Some(detyped)
      } catch {
        case t: Throwable =>
          // Swallow the error and keep going
          logger.warn(s"Error invoking computed parameter method ${tup._2}")
          None
      }
    }).toList

  final override def computedParameters(op: Parameterized, pvs: ParameterValues): Seq[ParameterValue] =
  /* super.computedParameters(pd) ++  */ methodsToParameters(pvs)
}
