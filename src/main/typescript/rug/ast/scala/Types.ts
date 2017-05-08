
/**
 * Module containing Scala types.
 * Interfaces with "Nav" suffix have 1:1 mapping to grammar productions
 * (with upper camel case).
 * They are purely a convenience for TypeScript users: Can be ignored in JavaScript.
 * Ultimately, such interfaces can be generated.
 *
 * Classes with "Ops" suffix are additional operations.
 */

import { TextTreeNode } from "../../tree/PathExpression";
import { TextTreeNodeOps } from "../TextTreeNodeOps";

// tslint:disable-next-line:no-empty-interface
export interface SourceNav extends TextTreeNode {

}

export class SourceOps extends TextTreeNodeOps<SourceNav> {

    /**
     * Return package name or null
     */
    public packageName(): TextTreeNode {
        try {
            return this.pexe.scalar<TextTreeNode, TextTreeNode>(this.node, "/pkg/termSelect");
        } catch (e) {
            return null;
        }
    }

    /**
     * Add the given import if it's not already imported
     */
    public addImport(newImport: string): void {
        const fullNewImport = `import ${newImport}`;

        if (this.node.value().indexOf(fullNewImport) === -1) {
            const pkg = this.packageName();
            if (pkg) {
                pkg.update(`${pkg.value()}\n\n${fullNewImport}`);
            } else {
                // Not in a package. Just put at the top of the class
                this.prepend(`${fullNewImport}\n\n`);
            }
        }
    }
}

/**
 * Represents a Scala source file
 */
export type Source = SourceNav & SourceOps;

export interface DefnClassNav extends TextTreeNode {

    typeName: string;

    template: TextTreeNode;

}

export class DefnClassOps extends TextTreeNodeOps<DefnClassNav> {

    /**
     * Add this content to the beginning of the body
     */
    public addToStartOfBody(what: string) {
        // TODO what if type body is empty?
        const firstStatement = this.node.template.children()[0] as TextTreeNode;
        firstStatement.update(`${what}\n\n${firstStatement.value()}`);
    }
}

export type DefnClass = DefnClassNav & DefnClassOps;

export interface TermApplyNav extends TextTreeNode {
    termName(): string;
}

export class TermApplyOps extends TextTreeNodeOps<TermApplyNav> {

}

export type TermApply = TermApplyNav & TermApplyOps;

export interface TermApplyInfixNav extends TextTreeNode {
    termSelect: any;
    termApply: any;
}

export class TermApplyInfixOps extends TextTreeNodeOps<TermApplyInfix> {

    public reverseShould() {
        const termSelect = this.node.termSelect;
        const termApply = this.node.termApply;

        if (termApply != null && ["be", "equal"].indexOf(termApply.termName.value()) > -1) {
            const newValue = `assert(${termSelect.value()} === ${termApply.children()[1].value()})`;
            this.node.update(newValue);
        }
    }

}

export type TermApplyInfix = TermApplyInfixNav & TermApplyInfixOps;
