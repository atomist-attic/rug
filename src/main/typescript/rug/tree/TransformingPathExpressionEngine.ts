
import { DynamicType, GraphNode, Match, PathExpression, PathExpressionEngine, TreeNode } from "./PathExpression";

/**
 * Convenient superclass that wraps an existing PathExpressionEngine
 * to decorate every returned node. Also adds convenience methods.
 */
export class TransformingPathExpressionEngine implements PathExpressionEngine {

  constructor(protected delegate: PathExpressionEngine,
              private nodeTransform: (TreeNode) => TreeNode) { }

  public addType(dt: DynamicType): this {
    this.delegate = this.delegate.addType(dt);
    return this;
  }

  // Unfortunately other calls don't go through this,
  // because they're in Scala
  public evaluate<R extends TreeNode, N extends TreeNode>(root: R, expr: PathExpression<R, N> | string): Match<R, N> {
    const m1 = this.delegate.evaluate(root, expr);
    const m2 = {
      root: this.nodeTransform(m1.root) as R,
      matches: m1.matches.map((n) => this.nodeTransform(n)) as any,
      pathExpressionEngine: m1.pathExpressionEngine,
      teamId: m1.teamId,
      contextRoot: m1.contextRoot,
      gitProjectLoader: m1.gitProjectLoader,
    };
    return m2;
  }

  public with<N extends TreeNode>(root: TreeNode, expr: PathExpression<GraphNode, N> | string,
                                  f: (n: N) => void): void {
    this.delegate.with(root, expr, (n) => {
      const transformed = this.nodeTransform(n);
      f(transformed as N);
    });
  }

  public scalar<R extends TreeNode, N extends TreeNode>(root: R, expr: PathExpression<R, N> | string): N {
    return this.nodeTransform(this.delegate.scalar<R, N>(root, expr)) as N;
  }

  public as<N extends TreeNode>(root, name: string): N {
    return this.nodeTransform(this.delegate.as<N>(root, name)) as N;
  }

  // Find the children of the current node of this time
  public children<N extends TreeNode>(root, name: string): N[] {
    return this.delegate.children<N>(root, name)
      .map((n) => this.nodeTransform(n) as N);
  }

  // -------------------------------------------------------------
  // Additional convenience methods
  // -------------------------------------------------------------
  public withExpression<N extends TreeNode>(root: TreeNode, pe: PathExpression<any, N>,
                                            f: (n: N) => void): void {
    this.with(root, pe.expression, f);
  }
}
