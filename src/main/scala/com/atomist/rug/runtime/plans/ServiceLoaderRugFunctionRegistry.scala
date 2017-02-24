package com.atomist.rug.runtime.plans

import java.util.ServiceLoader

import com.atomist.rug.RugRuntimeException
import com.atomist.rug.spi.{RugFunction, RugFunctionRegistry, Typed}
import com.typesafe.scalalogging.LazyLogging

import _root_.scala.collection.mutable.WeakHashMap
import scala.collection.JavaConverters._

/**
  * Use ServiceLoader to load RugFunction in to a registry
  */
class ServiceLoaderRugFunctionRegistry
  extends RugFunctionRegistry
    with LazyLogging {

  // Sharing Rug in a hierarchy of CLassLoaders requires the ServiceLoader to be triggered
  // for each ClassLoader; using WeakHashMap so ClassLoader references can be garbage collected
  private val functionsByClassLoader = WeakHashMap[ClassLoader, Map[String, RugFunction]]()

  private def functionsMap: Map[String, RugFunction] = {
    functionsByClassLoader.get(Thread.currentThread().getContextClassLoader) match {
      case Some(tm) => tm
      case _ =>
        val functionssMap: Map[String, RugFunction] = ServiceLoader.load(classOf[RugFunction]).asScala.map {
          case t: Typed =>
            logger.info(s"Registered rug function '${t.name}, with class ${t.getClass},description=${t.description}")
            t.name -> t
          case wtf =>
            throw new RugRuntimeException("RugFunction", s"RugFunction class ${wtf.getClass} must implement RugFunction interface", null)
        }.toMap
        functionsByClassLoader += Thread.currentThread().getContextClassLoader -> functionssMap
        functionssMap
    }
  }

  override def find(name: String): Option[RugFunction] = {
    functionsMap.get(name)
  }
}
