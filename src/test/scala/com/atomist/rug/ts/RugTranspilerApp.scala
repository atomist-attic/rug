package com.atomist.rug.ts

object RugTranspilerApp extends App {

  val transpiler = new RugTranspiler()


  val rug =
    """
      |editor PropertiesEdit
      |
      |with Properties p when path = "src/main/resources/application.properties"
      |do setProperty "server.port" "8181"
    """.stripMargin

  println(transpiler.transpile(rug))

}
