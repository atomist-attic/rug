import {Parameters} from "./Parameters"
import {Result} from "./Result"
import {Services} from "../model/Core"

export interface ReviewContext {

}

/**
 * Main entry point for cross project operations
 */
export interface Reviewer<P extends Parameters> {

    /**
     * Run against the given model
     */
    review(services: Services, rc: ReviewContext, p: P): Result

}
