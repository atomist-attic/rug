package com.atomist.rug.ts

import java.io.PrintWriter

import com.atomist.util.Utils
import org.apache.commons.io.IOUtils

object DefaultTypeGeneratorConfig {

  val CortexJsonLocation = "/com/atomist/rug/ts/cortex.json"

  lazy val CortexJson: String =
    IOUtils.toString(getClass.getResourceAsStream(CortexJsonLocation), "UTF-8")
}

/**
  * Intended to run as part of build
  */
object CortexTypeGeneratorApp extends App {

  import CortexTypeGenerator._
  import DefaultTypeGeneratorConfig._

  // TODO could take second argument as URL of endpoint

  val target = args.head
  val tsig = new CortexTypeGenerator(DefaultCortexDir, DefaultCortexStubDir)
  val output = tsig.toNodeModule(CortexJson)
  println(s"Generated Type module")
  output.allFiles.foreach(f => Utils.withCloseable(new PrintWriter(target + "/" + f.path))(_.write(f.content)))
}
