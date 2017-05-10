import { Microgrammar, GraphNode } from './PathExpression'

export interface MicrogrammarHelper {
    /*
     * Match the string starting at the first character.
     * If it doesn't match, a detailed report about why is returned instead.
     * No guarantee this report format will be the same in future versions
     * of Rug.
     */
    strictMatch(mg: Microgrammar, s: String): GraphNode | String

    /*
     * A string that describes the full match sequence a Microgrammar will look for.
     * Useful for debugging.
     */
    describe(mg: Microgrammar): String
}