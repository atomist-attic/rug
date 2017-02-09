package com.atomist.rug.kind.java

import com.atomist.rug.spi.TypeProvider

class JavaClassOrInterfaceProvider extends TypeProvider(classOf[JavaClassOrInterfaceView]) {

  override def description: String = "Java class or interface"
}
