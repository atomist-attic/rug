package com.atomist.rug.runtime.plans

import com.atomist.rug.spi.{RugFunction, RugFunctionRegistry}
import com.atomist.util.ServiceLoaderBackedExtensionProvider

/**
  * Use ServiceLoader to load RugFunction in to a registry
  */
class ServiceLoaderRugFunctionRegistry(keyProvider: (RugFunction) => String = (r) => r.name)
  extends ServiceLoaderBackedExtensionProvider[RugFunction] (keyProvider)
    with RugFunctionRegistry {

  override def find(name: String): Option[RugFunction] = {
    providerMap.get(name)
  }
}
