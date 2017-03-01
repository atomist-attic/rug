package com.atomist.rug.ts

object RugTranspilerApp extends App {

  val transpiler = new RugTranspiler()

  val rug =
    """
      |editor YamlEdit
      |
      |let group = $(/*[@name='x.yml']/YamlFile()/dependencies/*)
      |
      |with group g
      |     do update { g.value().replace("Death", "Life") } # Capitals are only present in the dependencies
    """.stripMargin

  println(transpiler.transpile(rug))

}
