package com.atomist.rug.runtime.js.v8

import java.io.File

import com.eclipsesource.v8._

/**
  * Created by kipz on 08/06/2017.
  */
object node {
  def main(args: Array[String]) {
    val nodeJS: NodeJS = NodeJS.createNodeJS
    val rug: V8Object = nodeJS.require(new File("/Users/kipz/scm/mackinac/spring-team-handlers/.atomist/target/ts-built/editors/PluginAdditionEditor.js"))

    while (nodeJS.isRunning) {
      {
        nodeJS.handleMessage
      }
    }

    val args = new V8Array(nodeJS.getRuntime)
    println(rug.getKeys.mkString(","))
    rug.get("uv").asInstanceOf[V8Object].executeFunction("edit",args)
    rug.release()
    nodeJS.release()
  }
}
