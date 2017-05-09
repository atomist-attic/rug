
type MicroMatcher = { kind: string } | string;

const Or = (alternatives: MicroMatcher[]): MicroMatcher => {
    const or = {
        kind: "or",
        components: alternatives,
    };
    return or;
};

export { MicroMatcher, Or };
