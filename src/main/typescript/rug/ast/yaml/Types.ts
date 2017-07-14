import { TextTreeNode } from "../../tree/PathExpression";

/**
 * Extended by classes that know how to handle YAML strings or sequences.
 */
export interface YamlStringOps {

    /**
     * Returns the actual text, not the raw value,
     * which may contain quotes etc.
     */
    text(): string;

    /**
     * Update the actual text, taking care of quotes, newlines etc.
     */
    updateText(to: string): void;
}

export type YamlString = YamlStringOps & TextTreeNode;

/**
 * String without quotes or newlines. Leave it alone.
 */
export class YamlRawValue implements YamlStringOps {

    constructor(private node: TextTreeNode) {
    }

    public text() {
        return this.node.value();
    }

    public updateText(to: string) {
        this.node.update(to);
    }
}

/**
 * String enclosed in "". Simply strips them.
 */
export class YamlQuotedValue implements YamlStringOps {

    constructor(private node: TextTreeNode) {
    }

    public text() {
        return this.node.value().slice(1, this.node.value().length - 1);
    }

    public updateText(to: string) {
        const newValue = `"${to}"`;
        this.node.update(newValue);
    }
}

/**
 * String using >. Strip newlines.
 */
export class YamlFoldedBlockScalar implements YamlStringOps {

    // Indent for each line
    private indent: string;
    private leading: string;

    constructor(private node: TextTreeNode) {
        const raw = node.value();
        if (raw.charAt(0) !== ">") { throw new Error("Illegal argument: Must begin with >"); }
        if (raw.charAt(1) !== "\n") {
            throw new Error("Illegal argument: Must begin with >\n");
        }
        let index = 2;
        let ch = raw.charAt(index);
        while (ch === " " || ch === "\t" || ch === "\n") {
            ch = raw.charAt(++index);
        }
        this.leading = raw.substr(0, index);
        this.indent = this.leading.substring(2);
    }

    public text() {
        return this.node.value().substr(this.leading.length)
            .replace(new RegExp("^" + this.indent, "mg"), "")
            .replace(new RegExp("\n+$"), "")
            .replace(new RegExp("([^\n])\n([^\n])", "g"), "$1 $2")
            .replace(new RegExp("\n\n", "g"), "\n") + "\n";
    }

    public updateText(to: string) {
        const newValue = `${this.leading}${to}`;
        // console.log(`Update from [${this.node.value()}] to [${newValue}]`)
        this.node.update(newValue);
    }
}

/**
 * String using >-.
 */
export class YamlFoldedBlockWithStripChomping implements YamlStringOps {

    // Indent for each line
    private indent: string;
    private leading: string;

    constructor(private node: TextTreeNode) {
        const raw = node.value();
        if (raw.charAt(0) !== ">" && raw.charAt(1) !== "-") { throw new Error("Illegal argument: Must begin with >-"); }
        if (raw.charAt(2) !== "\n") { throw new Error("Illegal argument: Must begin with >-\n"); }
        let index = 3;
        let ch = raw.charAt(index);
        while (ch === " " || ch === "\t" || ch === "\n") {
            ch = raw.charAt(++index);
        }
        this.leading = raw.substr(0, index);
        this.indent = this.leading.substring(3);
    }

    public text() {
        return this.node.value().substr(this.leading.length)
            .replace(new RegExp("^" + this.indent, "mg"), "")
            .replace(new RegExp("\n+$"), "")
            .replace(new RegExp("([^\n])\n([^\n])", "g"), "$1 $2")
            .replace(new RegExp("\n\n", "g"), "\n");
    }

    public updateText(to: string) {
        const newValue = `${this.leading}${to}`;
        // console.log(`Update from [${this.node.value()}] to [${newValue}]`)
        this.node.update(newValue);
    }
}

/**
 * String using >+.
 */
export class YamlFoldedBlockWithKeepChomping implements YamlStringOps {

    // Indent for each line
    private indent: string;
    private leading: string;

    constructor(private node: TextTreeNode) {
        const raw = node.value();
        if (raw.charAt(0) !== ">" && raw.charAt(1) !== "+") { throw new Error("Illegal argument: Must begin with >+"); }
        if (raw.charAt(2) !== "\n") { throw new Error("Illegal argument: Must begin with >+\n"); }
        let index = 3;
        let ch = raw.charAt(index);
        while (ch === " " || ch === "\t" || ch === "\n") {
            ch = raw.charAt(++index);
        }
        this.leading = raw.substr(0, index);
        this.indent = this.leading.substring(3);
    }

    public text() {
        const raw = this.node.value();
        const newlines = raw.match(new RegExp("\n+$"));
        return raw.substr(this.leading.length)
            .replace(new RegExp("^" + this.indent, "mg"), "")
            .replace(new RegExp("\n+$"), "")
            .replace(new RegExp("([^\n])\n([^\n])", "g"), "$1 $2")
            .replace(new RegExp("\n\n", "g"), "\n")
            + newlines[0].toString();
    }

    public updateText(to: string) {
        const newValue = `${this.leading}${to}`;
        // console.log(`Update from [${this.node.value()}] to [${newValue}]`)
        this.node.update(newValue);
    }
}

/**
 * String using |. Maintain newlines.
 */
export class YamlLiteralBlockScalar implements YamlStringOps {

    // Indent for each line
    private indent: string;
    private leading: string;

    constructor(private node: TextTreeNode) {
        const raw = node.value();
        if (raw.charAt(0) !== "|") { throw new Error("Illegal argument: Must begin with |"); }
        if (raw.charAt(1) !== "\n") { throw new Error("Illegal argument: Must begin with |\n"); }
        let index = 2;
        let ch = raw.charAt(index);
        while (ch === " " || ch === "\t" || ch === "\n") {
            ch = raw.charAt(++index);
        }
        this.leading = raw.substr(0, index);
        this.indent = this.leading.substring(2);
    }

    public text() {
        return this.node.value().substr(this.leading.length)
            .replace(new RegExp("^" + this.indent, "mg"), "")
            .replace(new RegExp("\n+$"), "\n");
    }

    public updateText(to: string) {
        const newValue = `${this.leading}${to}`;
        // console.log(`Update from [${this.node.value()}] to [${newValue}]`)
        this.node.update(newValue);
    }
}

/**
 * String using |-.
 */
export class YamlLiteralBlockWithStripChomping implements YamlStringOps {

    // Indent for each line
    private indent: string;
    private leading: string;

    constructor(private node: TextTreeNode) {
        const raw = node.value();
        if (raw.charAt(0) !== "|" && raw.charAt(1) !== "-") { throw new Error("Illegal argument: Must begin with |-"); }
        if (raw.charAt(2) !== "\n") { throw new Error("Illegal argument: Must begin with |-\n"); }
        let index = 3;
        let ch = raw.charAt(index);
        while (ch === " " || ch === "\t" || ch === "\n") {
            ch = raw.charAt(++index);
        }
        this.leading = raw.substr(0, index);
        this.indent = this.leading.substring(3);
    }

    public text() {
        return this.node.value().substr(this.leading.length)
            .replace(new RegExp("^" + this.indent, "mg"), "")
            .replace(new RegExp("\n+$"), "");
    }

    public updateText(to: string) {
        const newValue = `${this.leading}${to}`;
        // console.log(`Update from [${this.node.value()}] to [${newValue}]`)
        this.node.update(newValue);
    }
}

/**
 * String using |+.
 */
export class YamlLiteralBlockWithKeepChomping implements YamlStringOps {

    // Indent for each line
    private indent: string;
    private leading: string;

    constructor(private node: TextTreeNode) {
        const raw = node.value();
        if (raw.charAt(0) !== "|" && raw.charAt(1) !== "+") { throw new Error("Illegal argument: Must begin with |+"); }
        if (raw.charAt(2) !== "\n") { throw new Error("Illegal argument: Must begin with |+\n"); }
        let index = 3;
        let ch = raw.charAt(index);
        while (ch === " " || ch === "\t" || ch === "\n") {
            ch = raw.charAt(++index);
        }
        this.leading = raw.substr(0, index);
        this.indent = this.leading.substring(3);
    }

    public text() {
        const raw = this.node.value();
        const newlines = raw.match(new RegExp("\n+$"));
        return raw.substr(this.leading.length)
            .replace(new RegExp("^" + this.indent, "mg"), "")
            .replace(new RegExp("\n+$"), "")
            + newlines[0].toString();
    }

    public updateText(to: string) {
        const newValue = `${this.leading}${to}`;
        // console.log(`Update from [${this.node.value()}] to [${newValue}]`)
        this.node.update(newValue);
    }
}

/**
 * Works with a raw node value to add and remove sequence elements, observing YAML formatting.
 */
export class YamlSequenceOps {

    private items: string[];

    constructor(private node: TextTreeNode) {
        this.items = node.value().trim().split("\n");
        //console.log("Value: " + node.value());
    }

    /**
     * Adds an element to a sequence.
     *
     * @param elem the new element
     */
    public addElement(elem: string) {
        const lastItem = this.items[this.items.length - 1];
        this.items.push(lastItem.substring(0, lastItem.indexOf("-")) + "- " + elem);
        const newValue = this.items.join("\n") + "\n";
        this.node.update(newValue);
        // console.log(this.node.value())
    }

    /**
     * Removes an element, if it exists, from a sequence.
     *
     * @param elem the element to remove
     */
    public removeElement(elem: string) {
        const lastItem = this.items[this.items.length - 1];
        const index = this.items.indexOf(lastItem.substring(0, lastItem.indexOf("-")) + "- " + elem);
        if (index > -1) {
            this.items.splice(index, 1);
            const newValue = this.items.join("\n") + "\n";
            this.node.update(newValue);
            // console.log(this.node.value())
        }
    }

    /**
     * Update a key.
     *
     * @param the new key name
     */
    public updateKey(key: string) {
        this.node.update(key); // this currently only updates the value
    }
}

export type YamlSequence = YamlSequenceOps & TextTreeNode;
