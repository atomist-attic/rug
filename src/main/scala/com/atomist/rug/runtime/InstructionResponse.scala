package com.atomist.rug.runtime

import java.io.Serializable

/**
  * The response from Rug Functions
  */
case class InstructionResponse (status: String, code: Int, body: Serializable)
