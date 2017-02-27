package com.atomist.rug.ts

object RugTranspilerApp extends App {

  val transpiler = new RugTranspiler()


  val rug =
    """
      |editor Rename
      |
      |let dependencies = $(/*[@name='package.json']/Json()/dependencies)
      |
      |with dependencies
      | do addKeyValue "foo" "bar"
    """.stripMargin

  println(transpiler.transpile(rug))

}
