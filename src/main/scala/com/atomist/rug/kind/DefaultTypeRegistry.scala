package com.atomist.rug.kind

/**
  * Load types found on the classpath using ServiceLoader
  * Does not include Cortex types.
  */
object DefaultTypeRegistry extends ServiceLoaderTypeRegistry