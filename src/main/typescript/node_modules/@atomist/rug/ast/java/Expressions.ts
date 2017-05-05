import { Project } from "../../model/Core";
import { PathExpression, TextTreeNode } from "../../tree/PathExpression";

/**
 * Match a particular exception.
 */
export class Catch extends PathExpression<Project, TextTreeNode> {

    constructor(exception: string) {
        super(`//JavaFile()//catchClause//catchType[@value='${exception}']`);
    }
}

export const CatchException = new Catch("Exception");

export function annotatedClass(typeName: string) {
    return `normalClassDeclaration${withAnnotation(typeName)}`;
}

export function withAnnotation(typeName: string) {
    return `[/*/annotation//typeName[@value='${typeName}']]`;
}
