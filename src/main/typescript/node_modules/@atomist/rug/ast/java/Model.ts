import { RichTextTreeNode } from "../TextTreeNodeOps";

/**
 * Convenient class to wrap Java methods, hiding AST navigation.
 */
export class Method {

    public annotations: AnnotationWrapper[] = [];

    public filePath: string = this.methodDeclaration.containingFile().path;

    public name: string = this.nameNode().value();

    constructor(public methodDeclaration: MethodDeclaration) {
        if (methodDeclaration.methodModifier) {
            methodDeclaration.methodModifier
                .filter((mm) => mm.annotation !== undefined)
                .forEach((mm) =>
                    this.annotations.push(new AnnotationWrapper(mm.annotation)),
            );
        }

        if (methodDeclaration.methodHeader.annotation) {
            this.annotations.concat(methodDeclaration.methodHeader.annotation
                .map((ann) => new AnnotationWrapper(ann)));
        }
    }

    public rename(newName: string): void {
        this.nameNode().update(newName);
    }

    private nameNode(): RichTextTreeNode {
        return this.methodDeclaration.methodHeader.methodDeclarator.Identifier;
    }
}

export class AnnotationWrapper {

    public typeName: string;

    constructor(private annotation: Annotation) {
        if (this.annotation.markerAnnotation !== undefined) {
            this.typeName = this.annotation.markerAnnotation.typeName.value();
        } else if (this.annotation.normalAnnotation !== undefined) {
            this.typeName = this.annotation.normalAnnotation.typeName.value();
        } else {
            this.typeName = this.annotation.singleElementAnnotation.typeName.value();
        }
    }
}

// ------------------------------------------------------------------------
// Remaining classes reflect grammar. Not usually to be used directly.
// See Java8.g4 ANTLR grammar.
// ------------------------------------------------------------------------

/**
 * methodDeclaration
 * :	methodModifier* methodHeader methodBody
 */
export interface MethodDeclaration extends RichTextTreeNode {

    methodModifier: MethodModifier[];

    methodHeader: MethodHeader;

    methodBody: RichTextTreeNode;

}

/*
    methodModifier
    	:	annotation
    	|	'public'
    	|	'protected'
    	|	'private'
    	|	'abstract'
    	|	'static'
    	|	'final'
    	|	'synchronized'
    	|	'native'
    	|	'strictfp'
    	;
*/
export interface MethodModifier extends RichTextTreeNode {

    annotation: Annotation;

}

/*
    methodHeader
    	:	result methodDeclarator throws_?
    	|	typeParameters annotation* result methodDeclarator throws_?
    	;
*/
export interface MethodHeader extends RichTextTreeNode {

    result: RichTextTreeNode;

    methodDeclarator: MethodDeclarator;

    annotation: Annotation[];

}

/*
    methodDeclarator
    	:	Identifier '(' formalParameterList? ')' dims?
    	;
*/
export interface MethodDeclarator extends RichTextTreeNode {

    Identifier: RichTextTreeNode;

}

/*
    annotation
    	:	normalAnnotation
    	|	markerAnnotation
    	|	singleElementAnnotation
    	;

    normalAnnotation
    	:	'@' typeName '(' elementValuePairList? ')'
    	;
*/
export interface Annotation extends RichTextTreeNode {

    normalAnnotation;

    markerAnnotation;

    singleElementAnnotation;

    typeName;

}
