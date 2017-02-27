package com.atomist.rug.ts

object RugTranspilerApp extends App {

  val transpiler = new RugTranspiler()


  val rug =
    """
      |@description "I add Foobar annotations"
      |editor ClassAnnotated
      |
      |with SpringBootProject p
      |do
      |  annotateBootApplication "com.someone" "Foobar"
    """.stripMargin

  println(transpiler.transpile(rug))

}
