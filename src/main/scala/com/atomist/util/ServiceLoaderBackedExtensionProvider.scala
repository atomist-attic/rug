package com.atomist.util

import java.util.ServiceLoader

import com.atomist.rug.RugRuntimeException
import com.typesafe.scalalogging.LazyLogging

import scala.collection.JavaConverters._
import scala.reflect.{ClassTag, _}

/**
  * Provider of instances of type [[T]]. Instances are loaded via JDK [[ServiceLoader]] and cached; keyed by their
  * respective [[ClassLoader]]s.
  */
class ServiceLoaderBackedExtensionProvider[T: ClassTag](val keyProvider: T => String)
  extends LazyLogging {

  def providerMap: Map[String, T] = {
    ServiceLoader.load(classTag[T].runtimeClass).asScala.map {
      case t: T =>
        val key = keyProvider.apply(t)
        logger.info(s"Registered provider '$key' with class '${t.getClass}'")
        key -> t
      case wtf =>
        throw new RugRuntimeException("Extension", s"Provider class ${wtf.getClass} must implement ${classTag[T].runtimeClass.getName} interface", null)
    }.toMap
  }
}
