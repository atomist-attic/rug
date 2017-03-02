package com.atomist.rug.kind.java

import com.atomist.rug.spi.TypeProvider

class JavaClassOrInterfaceProvider extends TypeProvider(classOf[JavaClassOrInterfaceMutableView]) {

  override def description: String = "Java class or interface"
}
