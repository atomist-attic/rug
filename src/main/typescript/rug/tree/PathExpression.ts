import { HandlerContext } from "../operations/Handlers";

/**
 *  Returned when a PathExpression is evaluated. Also the context for an event handler.
 */
interface Match<R extends GraphNode, N extends GraphNode> extends HandlerContext {
    root: R;
    matches: N[];
}

/**
 * Object encapsulating a path expression. Facilitates reuse.
 */
class PathExpression<R extends GraphNode, N extends GraphNode> {
    constructor(readonly expression: string) { }
}

/**
 * Information about an element's position within a file.
 */
interface FormatInfo {

    /**
     * Information about the start of the element
     */
    start: PointFormatInfo;

    /**
     * Information about the end of the element
     */
    end: PointFormatInfo;
}

/**
 * Format and position information about a point within a file.
 */
interface PointFormatInfo {

    /**
     * Offset within the file, from 0
     */
    offset: number;

    lineNumberFrom1: number;

    columnNumberFrom1: number;

    /**
     * Number of indentation units at the point in the file
     */
    indentDepth: number;

    /**
     * Unit of indentation, such as "\t" or "   "
     */
    indent: string;
}

interface GraphNode {

    nodeName(): string;

    nodeTags(): string[];

}

/**
 * Operations common to all tree nodes.
 */
interface TreeNode extends GraphNode {

    children(): TreeNode[];
}

interface Addressed {

    /**
     * Path from root of graph or tree
     */
    address(): string;
}

interface AddressedGraphNode extends GraphNode, Addressed {

}

/**
 * Extended by TreeNodes that understand their place in hierarchy.
 */
interface ParentAwareTreeNode extends TreeNode {

    /**
     * Parent node. May be null, depending on the parent node.
     * AST-backed nodes will always have a non-null parent.
     */
    parent(): TreeNode;
}

/**
 * Additional operations on text based tree nodes.
 */
interface TextTreeNode extends ParentAwareTreeNode {

    /**
     * Information about the position of this node in the current file, if available.
     */
    formatInfo: FormatInfo;
    /**
     * String value of this node. If the node is a container,
     * will be built from the values of its descendants.
     */
    value(): string;

    /**
     * Update the node to the new value. Will invalidate existing children.
     * @param newValue new string value of the node.
     */
    update(newValue: string): void;

}

interface TypeProvider extends DynamicType {
    typeName: string;
    find(context: TreeNode): TreeNode[];
}

/*
 * Interface to execute path expressions and return Match objects.
 */
interface PathExpressionEngine {

    /**
     * Add a dynamic type. Can be used as a fluent API.
     */
    addType(dt: DynamicType): PathExpressionEngine;

    /**
     * Evaluate the given path expression
     */

    evaluate<R extends GraphNode, N extends GraphNode>(root: R, expr: PathExpression<R, N> | string): Match<R, N>;

    /**
     * Execute the given function on the nodes returned by the given path expression
     */
    with<N extends GraphNode>(root: GraphNode, expr: PathExpression<GraphNode, N> | string, f: (n: N) => void): void;

    /**
     * Return a single match. Throw an exception otherwise.
     */
    scalar<R extends GraphNode, N extends GraphNode>(root: R, expr: PathExpression<R, N> | string): N;

    /**
     * Cast the present node to the given type
     */
    as<N extends GraphNode>(root, name: string): N;

    /**
     * Find the children of the current node with the given name
     * @param name child name to look for
     */
    children<N extends GraphNode>(root, name: string): N[];
}

/**
 * Tag interface for dynamic types such as microgrammars
 */
// tslint:disable-next-line:no-empty-interface
interface DynamicType {

}

export { Match };
export { PathExpression };
export { PathExpressionEngine };
export { GraphNode };
export { Addressed };
export { AddressedGraphNode };
export { TreeNode };
export { ParentAwareTreeNode };
export { TextTreeNode };
export { FormatInfo };
export { DynamicType };
export { PointFormatInfo };
export { TypeProvider };
