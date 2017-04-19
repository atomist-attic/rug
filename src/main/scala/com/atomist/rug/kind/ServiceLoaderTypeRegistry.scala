package com.atomist.rug.kind

import com.atomist.rug.spi._
import com.atomist.util.ServiceLoaderBackedExtensionProvider

/**
  * Use JDK ServiceLocator to load Type classes. Each
  * JAR files needs a META-INF/services/com.atomist.rug.spi.Typed file containing
  * the FQNs of the types it defines.
  *
  * Does not include Cortex types: Only those backed by JVM objects
  * available on the runtime classpath.
  *
  * @see [[Type]]
  */
class ServiceLoaderTypeRegistry(keyProvider: Typed => String = r => r.name)
  extends ServiceLoaderBackedExtensionProvider[Typed](keyProvider)
    with TypeRegistry {

  override def findByName(kind: String): Option[Typed] = providerMap.get(kind)

  override def typeNames: Traversable[String] = providerMap.keys

  override def types: Seq[Typed] = providerMap.values.toSeq
}
