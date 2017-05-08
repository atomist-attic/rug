import { TreeNode } from "../../tree/PathExpression";
import { DecoratingPathExpressionEngine } from "../DecoratingPathExpressionEngine";

import * as scala from "./Types";

/**
 * PathExpressionEngine decorator that returns Scala type mixins
 * from all methods.
 */
export class ScalaPathExpressionEngine extends DecoratingPathExpressionEngine {

    protected decoratorFor(n: TreeNode): any {
        const className = this.decoratorClassName(n);
        // Dynamically instantiate the class with the given name in the
        // target module
        try {
            const ops = new scala[className](n, this.delegate);
            return ops;
        } catch (e) {
            // Not an error. We just don't have a special type for this
            return null;
        }
    }
}
