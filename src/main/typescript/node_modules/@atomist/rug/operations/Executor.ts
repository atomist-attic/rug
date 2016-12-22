import {Result,RugOperation} from "./RugOperation"
import {Services} from "../model/Core"

/**
 * Main entry point for cross project operations
 */
export interface Executor extends RugOperation{
  /**
   * Run against the given model
   */
  execute(services: Services, params: Object): Result
}
