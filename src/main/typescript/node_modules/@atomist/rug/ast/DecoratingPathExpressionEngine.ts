import { RichTextTreeNode, TextTreeNodeOps } from "../ast/TextTreeNodeOps";
import { PathExpressionEngine, TreeNode } from "../tree/PathExpression";
import { TransformingPathExpressionEngine } from "../tree/TransformingPathExpressionEngine";

/**
 * Can be used directly or via a subclass that provides a strategy
 * for resolving an ops class for a given node.
 * Tries to find an "Ops" class for the given node, and mix in
 * its methods with the node type methods. If an ops class can't
 * be found, mix in TextTreeNodeOps to return RichTextTreeNode.
 */
export class DecoratingPathExpressionEngine extends TransformingPathExpressionEngine {

    constructor(delegate: PathExpressionEngine) {
        super(delegate, (n) => {
            // Also need to parameterize module
            let ops = this.decoratorFor(n);
            if (ops == null) {
                ops = new TextTreeNodeOps(n, delegate);
            }
            const combined = this.unify(n, ops);
            // console.log(`ops=${ops}, combined=${combined}`)
            return combined;
        });
    }

    public save<N extends RichTextTreeNode>(n: TreeNode, expr: string): N[] {
        const hits = [];
        this.with(n, expr, (node) => {
            hits.push(node);
        });
        return hits;
    }
    /**
     * Template method subclasses can use to find the decorator for this node.
     * Implementations will typically use the decoratorClassName method.
     * Implementations don't need to catch the error if a class cannot
     * be instantiated: It will be caught in this class. They can also
     * return null if unresolved. Default implementation returns null.
     * @param n TreeNode to decorate
     */
    protected decoratorFor(n: TreeNode): any {
        return null;
    }

    /**
     * Convenience method returning the conventional decorator
     * class name for the node name. Simply adds "Ops" suffix.
     * @param n TreeNode we wish to decorate
     */
    protected decoratorClassName(n: TreeNode) {
        return n.nodeName().charAt(0).toUpperCase() + n.nodeName().substr(1) + "Ops";
    }

    /**
     * Add all functions from right to left.
     * Also copies state, which may be needed for functions to work.
     */
    private unify<T, U>(base: T, enricher: U): T & U {
        const monkeyableBase = base as any;
        // tslint:disable-next-line:forin
        for (const id in enricher) {
            const fun = (enricher as any)[id];
            monkeyableBase[id] = fun;
        }
        return monkeyableBase as T & U;
    }
}
