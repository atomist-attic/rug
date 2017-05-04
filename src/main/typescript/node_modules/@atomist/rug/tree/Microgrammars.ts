
type MicroMatcher = { kind: string } | String

let Or = function (alternatives: MicroMatcher[]): MicroMatcher {
    let or = {
        kind: "or",
        components: alternatives
    };
    return or;
};

let Regex = function (javaStyleRegex: string): MicroMatcher {
    let regex = { kind: "regex", regularExpression: javaStyleRegex };
    return regex;
}

let Repeat = function (what: MicroMatcher): MicroMatcher {
    let repeat = {
        kind: "repeat",
        what: what
    };
    return repeat;
}

let Optional = function (what: MicroMatcher): MicroMatcher {
    let opt = {
        kind: "optional",
        what: what
    };
    return opt;
}

let SkipAhead = function (to: MicroMatcher): MicroMatcher {
    let skipAhead = {
        kind: "break",
        to: to
    };
    return skipAhead;
}

/* this one is picky about whitespace */
let Literal = function (whitespaceSensitive: string): MicroMatcher {
    let opt = {
        kind: "strict-literal",
        content: whitespaceSensitive
    };
    return opt;
}

let Concat = function (components: MicroMatcher[]): MicroMatcher {
    let concat = {
        kind: "concat",
        components: components
    };
    return concat;
};

export { MicroMatcher, Or, Regex, Repeat, Optional, Literal, Concat, SkipAhead }