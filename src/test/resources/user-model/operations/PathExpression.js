"use strict";
/**
   Returned when a PathExpression is evaluated
*/
var Match = (function () {
    function Match(root, matches) {
        this._root = root;
        this._matches = matches;
    }
    // Nashorn needs methods
    Match.prototype.root = function () { return this._root; };
    Match.prototype.matches = function () { return this._matches; };
    return Match;
}());
exports.Match = Match;
var PathExpression = (function () {
    function PathExpression(expression) {
        this.expression = expression;
    }
    return PathExpression;
}());
exports.PathExpression = PathExpression;
