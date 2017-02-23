package com.atomist.rug.runtime.plans

import java.util.ServiceLoader

import com.atomist.rug.spi.{RugFunction, RugFunctionRegistry}
import com.typesafe.scalalogging.LazyLogging

import scala.collection.JavaConverters._

/**
  * Use ServiceLoader to load RugFunction in to a registry
  */
class ServiceLoaderRugFunctionRegistry
  extends RugFunctionRegistry with LazyLogging {

  private lazy val functions: Map[String, RugFunction] = {
    try{
      ServiceLoader.load(classOf[RugFunction]).asScala.map(r => (r.name, r)).toMap
    }catch {
      case x: Throwable =>
        x.printStackTrace()
        Map()
    }
  }

  override def find(name: String): Option[RugFunction] = {
    functions.get(name)
  }
}
