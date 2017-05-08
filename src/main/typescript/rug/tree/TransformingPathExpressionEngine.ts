import {PathExpressionEngine,PathExpression,Match,DynamicType,GraphNode,TreeNode} from "./PathExpression"

/**
 * Convenient superclass that wraps an existing PathExpressionEngine
 * to decorate every returned node. Also adds convenience methods.
 */
export class TransformingPathExpressionEngine implements PathExpressionEngine {

  constructor(protected delegate: PathExpressionEngine,
      private nodeTransform: (TreeNode) => TreeNode) {}

  addType(dt: DynamicType): this {
    this.delegate = this.delegate.addType(dt);
    return this
  }

  // Unfortunately other calls don't go through this,
  // because they're in Scala
  evaluate<R extends TreeNode,N extends TreeNode>(root: R, expr: PathExpression<R,N> | string): Match<R,N> {
    let m1 = this.delegate.evaluate(root, expr)
    let m2 = {
      root: this.nodeTransform(m1.root) as R,
      matches: m1.matches.map(n => this.nodeTransform(n)) as any,
      pathExpressionEngine: m1.pathExpressionEngine
    }
    return m2;
  }

  with<N extends TreeNode>(root: TreeNode, expr: PathExpression<GraphNode,N> | string,
            f: (n: N) => void): void {
    this.delegate.with(root, expr, n => {
        //console.log("Intercepted with")
        let transformed = this.nodeTransform(n)
        //console.log(`Transformed is ${transformed}`)
        f(transformed as N)
    })
  }

  scalar<R extends TreeNode,N extends TreeNode>(root: R, expr: PathExpression<R,N> | string): N {
    return this.nodeTransform(this.delegate.scalar<R,N>(root, expr)) as N
  }

  as<N extends TreeNode>(root, name: string): N {
    return this.nodeTransform(this.delegate.as<N>(root, name)) as N
  }

  // Find the children of the current node of this time
  children<N extends TreeNode>(root, name: string): N[] {
    return this.delegate.children<N>(root, name)
      .map(n => this.nodeTransform(n) as N)
  }


    //-------------------------------------------------------------
    // Additional convenience methods
    //-------------------------------------------------------------
    withExpression<N extends TreeNode>(root: TreeNode, pe: PathExpression<any,N>,
            f: (n: N) => void): void {
          this.with(root, pe.expression, f)
    }

}
