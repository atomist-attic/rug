interface Project {

    fileCount(): number

    addFile(path: string, content: string)

    files(): Array<File>

    copyFileOrFail(name: string, sourcePath: string, definitionPath: string)

    regexpReplace(a: string, b: string): void

    projects(): Array<Project>

    moveUnder(f: string): void


}

interface File {

    name(): string

    nameContains(what: string): boolean

    path(): string

    content(): string

    setContent(c: string): void

    append(what: string): void
    
    prepend(what: string): void

    isJava(): boolean

    regexpReplace(a: string, b: string): void

    replace(old: string, n: string): void

    lines(): Array<Line>
}

interface Line {

    num(): number
    content(): string
    update(l: string): void


}

export { Project }
export { File }
