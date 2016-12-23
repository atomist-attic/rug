package com.atomist.rug.kind

/**
  * Bootstrap Spring scanning. Use when we need to test a TypeRegistry without
  * other runtime elements.
  */
object DefaultTypeRegistry extends ServiceLoaderTypeRegistry {
}
