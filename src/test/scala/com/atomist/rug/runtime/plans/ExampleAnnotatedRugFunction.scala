package com.atomist.rug.runtime.plans

import com.atomist.rug.spi.Handlers.Status
import com.atomist.rug.spi.annotation.{Parameter, RugFunction, Secret, Tag}
import com.atomist.rug.spi.{AnnotatedRugFunction, FunctionResponse}

/**
  * For testing the annotation driven RugFunction api
  */
class ExampleAnnotatedRugFunction
  extends AnnotatedRugFunction {

  var blah: String = _
  var other: Int = -1
  var yum: Int = -1

  @RugFunction(name = "example-function", description = "Description of function", tags = Array(new Tag(name = "tag content")))
  def invoke(@Parameter(name = "number") number: Int,
             @Parameter(name = "blah", required = false) blah: String,
             @Parameter(name = "other", required = false) other: Int,
             @Parameter(name = "yum", required = false, defaultValue = "42") yum: Int,
             @Secret(name = "user_token", path = "github/user_token=repo") user_token: String): FunctionResponse = {
    this.blah = blah
    this.other = other
    this.yum = yum
    if (number == 100 && user_token == "woot") {
      FunctionResponse(Status.Success)
    } else {
      FunctionResponse(Status.Failure)
    }
  }
}

object ExampleAnnotatedRugFunction {
  val theName = "example-function"
  val theDescription = "Description of function"
  val theTag = "tag content"
  val theSecretPath = "github/user_token=repo"
}
