package com.atomist.rug.ts

object RugTranspilerApp extends App {

  val transpiler = new RugTranspiler()

  val rug =
    """
      |editor Replacer
      |
      |with Project
      |  do replace "org.springframework" "nonsense"
      |
      |with Replacer
      |   do replaceItNoGlobal "org.springframework" "nonsense"
    """.stripMargin

  println(transpiler.transpile(rug))

}
