import { AstHelper } from "./AstHelper";
import { File } from "../model/Core";
import { ReviewComment, Severity } from "../operations/RugOperation";
import { PathExpressionEngine, TextTreeNode } from "../tree/PathExpression";
import * as treeHelper from "../tree/TreeHelper";

/**
 * Base class for decorators on node.
 * Allows us to get to the containing File and update text.
 * Extended by specific classes, but also mixed in on its
 * own if we don't find a specific class.
 */
export class TextTreeNodeOps<N extends TextTreeNode> {

    protected astHelper: AstHelper;

    /**
     * Create a new TextTreeNodeOps, wrapping a given node.
     * Usually called by infrastructure, not end user.
     * @param node node to decorate
     * @param pexe expression engine that methods can use
     */
    constructor(protected node: N, public pexe: PathExpressionEngine) {
        this.astHelper = new AstHelper(pexe);
    }

    /**
     * Delete this node.
     */
    public delete() {
        this.node.update("");
    }

    /**
     * Append the given value to the content of this node.
     * Include whitespace or newline if you want it.
     * @param what content to append
     */
    public append(what: string) {
        this.node.update(`${this.node.value()}${what}`);
    }

    /**
     * Prepend the given value to the content of this node.
     * Include whitespace or newline if you want it.
     * @param what content to prepend
     */
    public prepend(what: string) {
        this.node.update(`${what}${this.node.value()}`);
    }

    /**
     * Return the file this node is contained in.
     */
    public containingFile(): File {
        return treeHelper.findAncestorWithTag<File>(this.node, "File");
    }

    /**
     * Return an issue concerning this, containing format information if available
     */
    public commentConcerning(comment: string, severity: Severity): ReviewComment {
        const f = this.containingFile();
        let line = 1;
        let col = 1;
        const point = this.node.formatInfo.start;
        if (point) {
            line = point.lineNumberFrom1;
            col = point.columnNumberFrom1;
        }
        return new ReviewComment(f.project.name, comment, severity, f.path, line, col);
    }

    /**
     * Add a comment to these ReviewResults
     */
    public addCommentConcerning(comments: ReviewComment[], comment: string, severity: Severity) {
        comments.push(this.commentConcerning(comment, severity));
    }

}

/**
 * Type we can enrich any TextTreeNode with, if there isn't a more specific type
 * containing additional operations.
 */
export type RichTextTreeNode = TextTreeNode & TextTreeNodeOps<TextTreeNode>;
