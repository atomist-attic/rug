package com.atomist.rug.kind.docker

case class DockerfileException(message: String, cause: Throwable) extends RuntimeException(message, cause)