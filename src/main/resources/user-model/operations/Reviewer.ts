import {RugOperation, Result, Parameter} from "./RugOperation"
import {Services} from "../model/Core"

export interface ReviewContext {

}

/**
 * Main entry point for cross project operations
 */
export interface Reviewer extends RugOperation {
  /**
   * Run against the given model
  */
  review(services: Services, rc: ReviewContext, ...args: string[]): Result
}
