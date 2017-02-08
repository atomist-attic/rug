package com.atomist.rug.runtime

/**
  * The response from Rugs, Executions etc
  */
trait InstructionResponse {
  def status: String
  def code: Int
  def body: Serializable
}
