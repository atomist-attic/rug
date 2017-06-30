package com.atomist.rug.kind

import com.atomist.rug.spi.SimpleTypeRegistry

/**
  * Load types found on the classpath using ServiceLoader
  * Does not include Cortex types.
  */
object DefaultTypeRegistry extends SimpleTypeRegistry(new ServiceLoaderTypeRegistry().types)