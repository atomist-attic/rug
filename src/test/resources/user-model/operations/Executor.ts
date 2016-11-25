import {Parameters} from "./Parameters"
import {Result} from "./Result"
import {Services} from "../model/Core"

/**
 * Main entry point for cross project operations
 */
export interface Executor<P extends Parameters> {

    /**
     * Run against the given model
     */
    execute(services: Services, p: P): Result

}
