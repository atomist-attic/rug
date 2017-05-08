
type MicroMatcher = { kind: string } | String

let Or = function(alternatives: MicroMatcher []): MicroMatcher {
    let or = {
        kind : "or",
        components: alternatives
    };
    return or;
};

export { MicroMatcher, Or }