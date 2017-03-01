package com.atomist.rug.ts

object RugTranspilerApp extends App {

  val transpiler = new RugTranspiler()

  val rug =
    """
      |editor Dude
      |
      |with Project p
      |do
      |  merge "template.vm" "dude.txt"
    """.stripMargin

  println(transpiler.transpile(rug))

}
