package com.atomist.rug.ts

object RugTranspilerApp extends App {

  val transpiler = new RugTranspiler()


  val rug =
    """
      |editor EveryPomEdit
      |with Project p
      |  with EveryPom o
      |    do setGroupId "mygroup"
    """.stripMargin

  println(transpiler.transpile(rug))

}
