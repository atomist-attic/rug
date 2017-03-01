package com.atomist.rug.runtime.plans

import com.atomist.rug.spi.{RugFunction, RugFunctionRegistry}
import com.atomist.util.ServiceLoaderBackedExtensionProvider

/**
  * Use ServiceLoader to load RugFunction into a registry
  */
class ServiceLoaderRugFunctionRegistry(keyProvider: (RugFunction) => String = (r) => r.getClass.getName)
  extends ServiceLoaderBackedExtensionProvider[RugFunction] (keyProvider)
    with RugFunctionRegistry {

  override def find(name: String): Option[RugFunction] = {
    providerMap.get(name) match {
      case Some(o) => Some(o)
      case _ =>
        providerMap.values.find(fn => fn.name == name)
    }
  }
}

object DefaultRugFunctionRegistry
  extends ServiceLoaderRugFunctionRegistry {
}
