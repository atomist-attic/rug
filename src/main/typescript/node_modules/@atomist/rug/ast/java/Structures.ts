import { DecoratingPathExpressionEngine } from "../DecoratingPathExpressionEngine";
import { RichTextTreeNode } from "../TextTreeNodeOps";

export function union(
    eng: DecoratingPathExpressionEngine,
    root: RichTextTreeNode,
    pe1: string,
    pe2: string): RichTextTreeNode[] {
    const hits1: RichTextTreeNode[] = eng.save<RichTextTreeNode>(root, pe1);
    const hits2: RichTextTreeNode[] = eng.save<RichTextTreeNode>(root, pe2);
    return hits1.concat(hits2);
}

/**
 * Convenient class to wrap Java methods, hiding AST navigation.
 */
export class Method {

    public annotations: RichTextTreeNode[] = [];
    public filePath: string = this.methodDeclaration.containingFile().path;
    public name: string = this.nameNode().value();

    constructor(private eng: DecoratingPathExpressionEngine, public methodDeclaration: RichTextTreeNode) {
        this.annotations = union(eng, this.methodDeclaration,
            "/methodHeader//annotation",
            "//methodModifier//annotation",
        );
    }

    public rename(newName: string): void {
        this.nameNode().update(newName);
    }
    private nameNode(): RichTextTreeNode {
        return this.eng.scalar<RichTextTreeNode, RichTextTreeNode>(this.methodDeclaration,
            "/methodHeader/methodDeclarator/Identifier");
    }
}
