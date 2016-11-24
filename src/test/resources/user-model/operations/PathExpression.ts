
/**
   Returned when a PathExpression is evaluated
*/
class Match<R,N> {

  _root: R;
  _matches: Array<N>;

  constructor(root: R, matches: Array<N>) {
    this._root = root
    this._matches = matches;
  }

  root(): R { return this._root; }
  matches(): Array<N> { return this._matches;}
}

class PathExpression<R,N> {

  constructor(public expression: string) {}

}

interface PathExpressionEngine {

  evaluate<R,N>(root, expr: PathExpression<R,N>): Match<R,N>
}

export {Match}
export {PathExpression}
export {PathExpressionEngine}
