package com.atomist.rug.ts

object RugTranspilerApp extends App {

  val transpiler = new RugTranspiler()

  val rug =
    """
      |@description "Update Kube spec to redeploy a service"
      |editor Redeploy
      |
      |param service: ^[\w.\-_]+$
      |param new_sha: ^[a-f0-9]{7}$
      |
      |with Project p
      |do
      |  regexpReplace { return service + ":[a-f0-9]{7}" } { service + ":" + new_sha };
    """.stripMargin

  println(transpiler.transpile(rug))

}
