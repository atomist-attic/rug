package com.atomist.rug.ts

object RugTranspilerApp extends App {

  val transpiler = new RugTranspiler()

  val rug =
    """
      |reviewer FindSecrets
      |
      |#let secret = ""
      |
      |with File f when { f.name().endsWith('yml') }
      |	do eval {
      |     var secret = "";
      |     var matches = f.content().match(secret);
      |     for ( i = 0; i < matches.length; i++)
      |       f.majorProblem(matches[i], ic);
      |     return null;
      | }
    """.stripMargin

  println(transpiler.transpile(rug))

}
