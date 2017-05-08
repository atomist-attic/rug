import { File } from "../model/Core";
import {
    ParentAwareTreeNode,
    PathExpressionEngine,
    TextTreeNode,
    TreeNode,
} from "../tree/PathExpression";
import * as treeHelper from "../tree/TreeHelper";

/**
 * Superclass for working with ASTs
 */
export class AstHelper {

    public constructor(public pexe: PathExpressionEngine) { }

    /**
     * Reparse this file, given the type and return the top level node.
     */
    protected reparseNodeUnderFile(languageNode: TextTreeNode, type: string): TextTreeNode {
        const f = treeHelper.findAncestorWithTag<File>(languageNode, "File");
        if (f) {
            const pathExpression = `/${type}()`;
            const r = this.pexe.scalar<File, TextTreeNode>(f, pathExpression);
            return r;
        } else {
            throw new Error(`Cannot find File parent`);
        }
    }
}
