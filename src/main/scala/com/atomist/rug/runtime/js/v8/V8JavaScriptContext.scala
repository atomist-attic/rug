package com.atomist.rug.runtime.js.v8

import com.eclipsesource.v8.{NodeJS, V8}

/**
  * Use V8
  */
object V8JavaScriptContext {
  def main(args: Array[String]) {
    val runtime = V8.createV8Runtime

    val result: Int = runtime.executeIntegerScript("" + "var hello = 'hello, ';\n" + "var world = 'world!';\n" + "hello.concat(world).length;\n")
    System.out.println(result)
    runtime.release()
  }
}
