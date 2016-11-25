
/*
* Licensed under the Apache License v 2.0
*/


/*
 * Docker file type
 */
interface Dockerfile {

    //addContents: string
    addAdd(addContents: string): void

    //copyContents: string
    addCopy(copyContents: string): void

    //envContents: string
    addEnv(envContents: string): void

    //exposeContents: string
    addExpose(exposeContents: string): void

    //labelContents: string
    addLabel(labelContents: string): void

    //maintainerName: string
    //maintainerEmail: string
    addMaintainer(maintainerName: string, maintainerEmail: string): void

    //cmdContents: string
    addOrUpdateCmd(cmdContents: string): void

    //entrypointContent: string
    addOrUpdateEntryPoint(entrypointContent: string): void

    //exposeContents: string
    addOrUpdateExpose(exposeContents: string): void

    //fromContents: string
    addOrUpdateFrom(fromContents: string): void

    //labelContents: string
    addOrUpdateLabel(labelContents: string): void

    //maintainerName: string
    //maintainerEmail: string
    addOrUpdateMaintainer(maintainerName: string, maintainerEmail: string): void

    //workdirContents: string
    addOrUpdateWorkdir(workdirContents: string): void

    //runContents: string
    addRun(runContents: string): void

    //volumeContents: string
    addVolume(volumeContents: string): void


    content(): string

    //arg0: any
    eval(arg0: any): void

    //msg: string
    fail(msg: string): void


    filename(): string


    isWellFormed(): boolean


    lineCount(): number


    path(): string

    //msg: string
    println(msg: string): void

    //root: string
    underPath(root: string): boolean

}   // interface Dockerfile


/*
 * Elm module
 */
interface ElmModule {

    //arg0: string
    addFunction(arg0: string): void

    //importStatement: string
    addImportStatement(importStatement: string): void


    content(): string

    //arg0: any
    eval(arg0: any): void

    //name: string
    exposes(name: string): boolean

    //msg: string
    fail(msg: string): void


    filename(): string

    //moduleName: string
    imports(moduleName: string): boolean


    isWellFormed(): boolean


    lineCount(): number


    name(): string


    path(): string

    //msg: string
    println(msg: string): void

    //arg0: string
    removeFunction(arg0: string): void

    //newName: string
    rename(newName: string): void

    //newExposing: string
    replaceExposing(newExposing: string): void

    //root: string
    underPath(root: string): boolean

    //oldModuleName: string
    //newName: string
    updateImport(oldModuleName: string, newName: string): void

}   // interface ElmModule


/*
 *
Type for a file within a project. Supports generic options such as find and replace.

 */
interface File {

    //literal: string
    append(literal: string): void

    //what: string
    contains(what: string): boolean

    //regexp: string
    containsMatch(regexp: string): boolean


    content(): string

    //arg0: any
    eval(arg0: any): void

    //msg: string
    fail(msg: string): void


    filename(): string

    //regexp: string
    findMatches(regexp: string): any[]

    //regexp: string
    firstMatch(regexp: string): string


    isJava(): boolean


    isWellFormed(): boolean


    lineCount(): number


    name(): string

    //what: string
    nameContains(what: string): boolean


    path(): string

    //literal: string
    prepend(literal: string): void

    //msg: string
    println(msg: string): void

    //regexp: string
    //replaceWith: string
    regexpReplace(regexp: string, replaceWith: string): void

    //literal: string
    //replaceWith: string
    replace(literal: string, replaceWith: string): void

    //newContent: string
    setContent(newContent: string): void

    //name: string
    setName(name: string): void

    //newPath: string
    setPath(newPath: string): void

    //root: string
    underPath(root: string): boolean

}   // interface File


/*
 * Execute http calls
 */
interface Http {

    //arg0: any
    eval(arg0: any): void

    //msg: string
    fail(msg: string): void

    //url: string
    //data: string
    postJson(url: string, data: string): void

    //msg: string
    println(msg: string): void

    //url: string
    //data: string
    putJson(url: string, data: string): void

}   // interface Http


/*
 * Java class
 */
interface JavaClass {

    //pkg: string
    //annotation: string
    addAnnotation(pkg: string, annotation: string): void

    //fqn: string
    addImport(fqn: string): void

    //arg0: any
    eval(arg0: any): void

    //msg: string
    fail(msg: string): void

    //annotation: string
    hasAnnotation(annotation: string): boolean

    //arg0: string
    inheritsFrom(arg0: string): boolean


    isAbstract(): boolean


    isInterface(): boolean


    lineCount(): number

    //newPackage: string
    movePackage(newPackage: string): void


    name(): string


    pkg(): string

    //msg: string
    println(msg: string): void

    //newName: string
    rename(newName: string): void

    //target: string
    //replacement: string
    renameByReplace(target: string, replacement: string): void

    //arg0: string
    setHeaderComment(arg0: string): void

}   // interface JavaClass


/*
 * Java project
 */
interface JavaProject {

    //name: string
    //parentPath: string
    addDirectory(name: string, parentPath: string): void

    //directoryPath: string
    addDirectoryAndIntermediates(directoryPath: string): void

    //path: string
    //content: string
    addFile(path: string, content: string): void

    //sourcePath: string
    copyEditorBackingFileOrFail(sourcePath: string): void

    //sourcePath: string
    //destinationPath: string
    copyEditorBackingFileOrFail(sourcePath: string, destinationPath: string): void

    //sourcePath: string
    //destinationPath: string
    copyEditorBackingFilesOrFail(sourcePath: string, destinationPath: string): void

    //sourcePath: string
    copyEditorBackingFilesPreservingPath(sourcePath: string): void

    //sourcePath: string
    //destinationPath: string
    copyEditorBackingFilesWithNewRelativePath(sourcePath: string, destinationPath: string): void

    //sourcePath: string
    //destinationPath: string
    copyFile(sourcePath: string, destinationPath: string): void

    //sourcePath: string
    //destinationPath: string
    copyFileOrFail(sourcePath: string, destinationPath: string): void

    //path: string
    countFilesInDirectory(path: string): number

    //path: string
    deleteDirectory(path: string): void

    //path: string
    deleteFile(path: string): void

    //path: string
    directoryExists(path: string): boolean

    //arg0: any
    eval(arg0: any): void

    //msg: string
    fail(msg: string): void

    //path: string
    //content: string
    fileContains(path: string, content: string): boolean


    fileCount(): number

    //path: string
    fileExists(path: string): boolean

    //path: string
    //content: string
    fileHasContent(path: string, content: string): boolean


    files(): any[]


    isMaven(): boolean


    isSpring(): boolean


    isSpringBoot(): boolean


    javaFileCount(): number

    //path: string
    moveUnder(path: string): void


    name(): string


    packages(): any[]

    //msg: string
    println(msg: string): void


    projects(): any[]

    //regexp: string
    //replacement: string
    regexpReplace(regexp: string, replacement: string): void

    //oldPackage: string
    //newPackage: string
    renamePackage(oldPackage: string, newPackage: string): void

    //literal: string
    //replaceWith: string
    replace(literal: string, replaceWith: string): void

    //literal: string
    //replacement: string
    replaceInPath(literal: string, replacement: string): void

}   // interface JavaProject


/*
 * Java source file
 */
interface JavaSource {


    content(): string

    //arg0: any
    eval(arg0: any): void

    //msg: string
    fail(msg: string): void


    filename(): string


    isWellFormed(): boolean


    lineCount(): number

    //newPackage: string
    movePackage(newPackage: string): void


    path(): string


    pkg(): string

    //msg: string
    println(msg: string): void


    typeCount(): number

    //root: string
    underPath(root: string): boolean

}   // interface JavaSource


/*
 * package.json configuration file
 */
interface Json {


    content(): string

    //arg0: any
    eval(arg0: any): void

    //msg: string
    fail(msg: string): void


    filename(): string


    isWellFormed(): boolean


    lineCount(): number


    path(): string

    //msg: string
    println(msg: string): void

    //root: string
    underPath(root: string): boolean

}   // interface Json


/*
 * Represents a line within a text file
 */
interface Line {


    content(): string

    //arg0: any
    eval(arg0: any): void

    //msg: string
    fail(msg: string): void


    num(): number

    //msg: string
    println(msg: string): void

    //s2: string
    update(s2: string): void

}   // interface Line


/*
 *
Type for a file within a project. Supports generic options such as find and replace.

 */
interface File {

    //literal: string
    append(literal: string): void

    //what: string
    contains(what: string): boolean

    //regexp: string
    containsMatch(regexp: string): boolean


    content(): string

    //arg0: any
    eval(arg0: any): void

    //msg: string
    fail(msg: string): void


    filename(): string

    //regexp: string
    findMatches(regexp: string): any[]

    //regexp: string
    firstMatch(regexp: string): string


    isJava(): boolean


    isWellFormed(): boolean


    lineCount(): number


    name(): string

    //what: string
    nameContains(what: string): boolean


    path(): string

    //literal: string
    prepend(literal: string): void

    //msg: string
    println(msg: string): void

    //regexp: string
    //replaceWith: string
    regexpReplace(regexp: string, replaceWith: string): void

    //literal: string
    //replaceWith: string
    replace(literal: string, replaceWith: string): void

    //newContent: string
    setContent(newContent: string): void

    //name: string
    setName(name: string): void

    //newPath: string
    setPath(newPath: string): void

    //root: string
    underPath(root: string): boolean

}   // interface File


/*
 * package.json configuration file
 */
interface PackageJSON {

    //literal: string
    append(literal: string): void

    //what: string
    contains(what: string): boolean

    //regexp: string
    containsMatch(regexp: string): boolean


    content(): string

    //arg0: any
    eval(arg0: any): void

    //msg: string
    fail(msg: string): void


    filename(): string

    //regexp: string
    findMatches(regexp: string): any[]

    //regexp: string
    firstMatch(regexp: string): string


    isJava(): boolean


    isWellFormed(): boolean


    lineCount(): number


    name(): string

    //what: string
    nameContains(what: string): boolean


    path(): string

    //literal: string
    prepend(literal: string): void

    //msg: string
    println(msg: string): void

    //regexp: string
    //replaceWith: string
    regexpReplace(regexp: string, replaceWith: string): void

    //literal: string
    //replaceWith: string
    replace(literal: string, replaceWith: string): void

    //newContent: string
    setContent(newContent: string): void

    //name: string
    setName(name: string): void

    //newPath: string
    setPath(newPath: string): void

    //root: string
    underPath(root: string): boolean

}   // interface PackageJSON


/*
 * XML file
 */
interface Xml {

    //xpath: string
    //newNode: string
    //nodeContent: string
    addChildNode(xpath: string, newNode: string, nodeContent: string): void

    //parentNodeXPath: string
    //xPathOfNodeToReplace: string
    //newNode: string
    //nodeContent: string
    addOrReplaceNode(parentNodeXPath: string, xPathOfNodeToReplace: string, newNode: string, nodeContent: string): void

    //xpath: string
    contains(xpath: string): boolean


    content(): string

    //xpath: string
    deleteNode(xpath: string): void

    //arg0: any
    eval(arg0: any): void

    //msg: string
    fail(msg: string): void


    filename(): string

    //xpath: string
    getTextContentFor(xpath: string): string


    isWellFormed(): boolean


    lineCount(): number


    path(): string

    //msg: string
    println(msg: string): void

    //xpath: string
    //arg1: string
    setTextContentFor(xpath: string, arg1: string): void

    //root: string
    underPath(root: string): boolean

}   // interface Xml


/*
 * POM XML file
 */
interface Pom {

    //xpath: string
    //newNode: string
    //nodeContent: string
    addChildNode(xpath: string, newNode: string, nodeContent: string): void

    //groupId: string
    //artifactId: string
    //pluginContent: string
    addOrReplaceBuildPlugin(groupId: string, artifactId: string, pluginContent: string): void

    //groupId: string
    //artifactId: string
    addOrReplaceDependency(groupId: string, artifactId: string): void

    //groupId: string
    //artifactId: string
    //dependencyContent: string
    addOrReplaceDependencyManagementDependency(groupId: string, artifactId: string, dependencyContent: string): void

    //groupId: string
    //artifactId: string
    //newVersion: string
    addOrReplaceDependencyOfVersion(groupId: string, artifactId: string, newVersion: string): void

    //groupId: string
    //artifactId: string
    //newVersion: string
    addOrReplaceDependencyScope(groupId: string, artifactId: string, newVersion: string): void

    //groupId: string
    //artifactId: string
    //newVersion: string
    addOrReplaceDependencyVersion(groupId: string, artifactId: string, newVersion: string): void

    //parentNodeXPath: string
    //xPathOfNodeToReplace: string
    //newNode: string
    //nodeContent: string
    addOrReplaceNode(parentNodeXPath: string, xPathOfNodeToReplace: string, newNode: string, nodeContent: string): void

    //propertyName: string
    //propertyValue: string
    addOrReplaceProperty(propertyName: string, propertyValue: string): void


    artifactId(): string

    //xpath: string
    contains(xpath: string): boolean


    content(): string

    //xpath: string
    deleteNode(xpath: string): void

    //groupId: string
    //artifactId: string
    dependencyScope(groupId: string, artifactId: string): string

    //groupId: string
    //artifactId: string
    dependencyVersion(groupId: string, artifactId: string): string


    description(): string

    //arg0: any
    eval(arg0: any): void

    //msg: string
    fail(msg: string): void


    filename(): string

    //xpath: string
    getTextContentFor(xpath: string): string


    groupId(): string

    //groupId: string
    //artifactId: string
    isBuildPluginPresent(groupId: string, artifactId: string): boolean

    //groupId: string
    //artifactId: string
    isDependencyManagementDependencyPresent(groupId: string, artifactId: string): boolean

    //groupId: string
    //artifactId: string
    isDependencyPresent(groupId: string, artifactId: string): boolean


    isWellFormed(): boolean


    lineCount(): number


    name(): string


    packaging(): string


    parentArtifactId(): string


    parentGroupId(): string


    parentVersion(): string


    path(): string

    //msg: string
    println(msg: string): void

    //projectPropertyName: string
    property(projectPropertyName: string): string

    //groupId: string
    //artifactId: string
    removeDependency(groupId: string, artifactId: string): void

    //groupId: string
    //artifactId: string
    removeDependencyScope(groupId: string, artifactId: string): void

    //groupId: string
    //artifactId: string
    removeDependencyVersion(groupId: string, artifactId: string): void

    //propertyName: string
    removeProperty(propertyName: string): void

    //newParentBlock: string
    replaceParent(newParentBlock: string): void

    //newArtifactId: string
    setArtifactId(newArtifactId: string): void

    //newDescription: string
    setDescription(newDescription: string): void

    //newGroupId: string
    setGroupId(newGroupId: string): void

    //newPackaging: string
    setPackaging(newPackaging: string): void

    //newParentArtifactId: string
    setParentArtifactId(newParentArtifactId: string): void

    //newParentGroupId: string
    setParentGroupId(newParentGroupId: string): void

    //newParentVersion: string
    setParentVersion(newParentVersion: string): void

    //newName: string
    setProjectName(newName: string): void

    //xpath: string
    //arg1: string
    setTextContentFor(xpath: string, arg1: string): void

    //newVersion: string
    setVersion(newVersion: string): void

    //root: string
    underPath(root: string): boolean


    version(): string

}   // interface Pom


/*
 *
Type for a project. Supports global operations.
Consider using file and other lower types by preference as project
operations can be inefficient.

 */
interface Project {

    //name: string
    //parentPath: string
    addDirectory(name: string, parentPath: string): void

    //directoryPath: string
    addDirectoryAndIntermediates(directoryPath: string): void

    //path: string
    //content: string
    addFile(path: string, content: string): void

    //sourcePath: string
    copyEditorBackingFileOrFail(sourcePath: string): void

    //sourcePath: string
    //destinationPath: string
    copyEditorBackingFileOrFail(sourcePath: string, destinationPath: string): void

    //sourcePath: string
    //destinationPath: string
    copyEditorBackingFilesOrFail(sourcePath: string, destinationPath: string): void

    //sourcePath: string
    copyEditorBackingFilesPreservingPath(sourcePath: string): void

    //sourcePath: string
    //destinationPath: string
    copyEditorBackingFilesWithNewRelativePath(sourcePath: string, destinationPath: string): void

    //sourcePath: string
    //destinationPath: string
    copyFile(sourcePath: string, destinationPath: string): void

    //sourcePath: string
    //destinationPath: string
    copyFileOrFail(sourcePath: string, destinationPath: string): void

    //path: string
    countFilesInDirectory(path: string): number

    //path: string
    deleteDirectory(path: string): void

    //path: string
    deleteFile(path: string): void

    //path: string
    directoryExists(path: string): boolean

    //arg0: any
    eval(arg0: any): void

    //msg: string
    fail(msg: string): void

    //path: string
    //content: string
    fileContains(path: string, content: string): boolean


    fileCount(): number

    //path: string
    fileExists(path: string): boolean

    //path: string
    //content: string
    fileHasContent(path: string, content: string): boolean


    files(): any[]

    //path: string
    moveUnder(path: string): void


    name(): string

    //msg: string
    println(msg: string): void


    projects(): any[]

    //regexp: string
    //replacement: string
    regexpReplace(regexp: string, replacement: string): void

    //literal: string
    //replaceWith: string
    replace(literal: string, replaceWith: string): void

    //literal: string
    //replacement: string
    replaceInPath(literal: string, replacement: string): void

}   // interface Project


/*
 * Properties file
 */
interface Properties {

    //key: string
    containsKey(key: string): boolean

    //value: string
    containsValue(value: string): boolean


    content(): string

    //arg0: any
    eval(arg0: any): void

    //msg: string
    fail(msg: string): void


    filename(): string

    //key: string
    getValue(key: string): string


    isWellFormed(): boolean


    keys(): any[]


    lineCount(): number


    path(): string

    //msg: string
    println(msg: string): void

    //key: string
    //value: string
    setProperty(key: string, value: string): void

    //root: string
    underPath(root: string): boolean

}   // interface Properties


/*
 * Python file
 */
interface Python {

    //arg0: string
    append(arg0: string): void

    //arg0: any
    eval(arg0: any): void

    //msg: string
    fail(msg: string): void

    //msg: string
    println(msg: string): void

    //key: string
    //value: string
    set(key: string, value: string): void

    //name: string
    valueOf(name: string): any

}   // interface Python


/*
 * Python requirements file
 */
interface PythonRequirements {

    //arg0: string
    append(arg0: string): void

    //arg0: any
    eval(arg0: any): void

    //msg: string
    fail(msg: string): void

    //msg: string
    println(msg: string): void

    //key: string
    //value: string
    set(key: string, value: string): void

    //name: string
    valueOf(name: string): any

}   // interface PythonRequirements


/*
 * Python requirements text file
 */
interface PythonRequirementsTxt {

    //arg0: string
    append(arg0: string): void

    //arg0: any
    eval(arg0: any): void

    //msg: string
    fail(msg: string): void

    //msg: string
    println(msg: string): void

    //key: string
    //value: string
    set(key: string, value: string): void

    //name: string
    valueOf(name: string): any

}   // interface PythonRequirementsTxt


/*
 * Test type for replacing the content of files
 */
interface Replacer {


    content(): string

    //arg0: any
    eval(arg0: any): void

    //msg: string
    fail(msg: string): void


    filename(): string


    isWellFormed(): boolean


    lineCount(): number

    //p1: string
    //p2: string
    overloaded(p1: string, p2: string): void

    //p1: string
    overloaded(p1: string): void


    path(): string

    //msg: string
    println(msg: string): void

    //stringToReplace: string
    //stringReplacement: string
    replaceIt(stringToReplace: string, stringReplacement: string): void

    //stringToReplace: string
    //stringReplacement: string
    replaceItNoGlobal(stringToReplace: string, stringReplacement: string): void

    //root: string
    underPath(root: string): boolean

}   // interface Replacer


/*
 * Test type for replacing the content of clojure files
 */
interface Replacerclj {


    content(): string

    //arg0: any
    eval(arg0: any): void

    //msg: string
    fail(msg: string): void


    filename(): string


    isWellFormed(): boolean


    lineCount(): number

    //p1: string
    //p2: string
    overloaded(p1: string, p2: string): void

    //p1: string
    overloaded(p1: string): void


    path(): string

    //msg: string
    println(msg: string): void

    //stringToReplace: string
    //stringReplacement: string
    replaceIt(stringToReplace: string, stringReplacement: string): void

    //stringToReplace: string
    //stringReplacement: string
    replaceItNoGlobal(stringToReplace: string, stringReplacement: string): void

    //root: string
    underPath(root: string): boolean

}   // interface Replacerclj


/*
 *
Type for a project. Supports global operations.
Consider using file and other lower types by preference as project
operations can be inefficient.

 */
interface Project {

    //name: string
    //parentPath: string
    addDirectory(name: string, parentPath: string): void

    //directoryPath: string
    addDirectoryAndIntermediates(directoryPath: string): void

    //path: string
    //content: string
    addFile(path: string, content: string): void

    //sourcePath: string
    copyEditorBackingFileOrFail(sourcePath: string): void

    //sourcePath: string
    //destinationPath: string
    copyEditorBackingFileOrFail(sourcePath: string, destinationPath: string): void

    //sourcePath: string
    //destinationPath: string
    copyEditorBackingFilesOrFail(sourcePath: string, destinationPath: string): void

    //sourcePath: string
    copyEditorBackingFilesPreservingPath(sourcePath: string): void

    //sourcePath: string
    //destinationPath: string
    copyEditorBackingFilesWithNewRelativePath(sourcePath: string, destinationPath: string): void

    //sourcePath: string
    //destinationPath: string
    copyFile(sourcePath: string, destinationPath: string): void

    //sourcePath: string
    //destinationPath: string
    copyFileOrFail(sourcePath: string, destinationPath: string): void

    //path: string
    countFilesInDirectory(path: string): number

    //path: string
    deleteDirectory(path: string): void

    //path: string
    deleteFile(path: string): void

    //path: string
    directoryExists(path: string): boolean

    //arg0: any
    eval(arg0: any): void

    //msg: string
    fail(msg: string): void

    //path: string
    //content: string
    fileContains(path: string, content: string): boolean


    fileCount(): number

    //path: string
    fileExists(path: string): boolean

    //path: string
    //content: string
    fileHasContent(path: string, content: string): boolean


    files(): any[]

    //path: string
    moveUnder(path: string): void


    name(): string

    //msg: string
    println(msg: string): void


    projects(): any[]

    //regexp: string
    //replacement: string
    regexpReplace(regexp: string, replacement: string): void

    //literal: string
    //replaceWith: string
    replace(literal: string, replaceWith: string): void

    //literal: string
    //replacement: string
    replaceInPath(literal: string, replacement: string): void

}   // interface Project


/*
 *
Type for services. Used in executors.

 */
interface Services {

    //name: string
    //parentPath: string
    addDirectory(name: string, parentPath: string): void

    //directoryPath: string
    addDirectoryAndIntermediates(directoryPath: string): void

    //path: string
    //content: string
    addFile(path: string, content: string): void

    //sourcePath: string
    copyEditorBackingFileOrFail(sourcePath: string): void

    //sourcePath: string
    //destinationPath: string
    copyEditorBackingFileOrFail(sourcePath: string, destinationPath: string): void

    //sourcePath: string
    //destinationPath: string
    copyEditorBackingFilesOrFail(sourcePath: string, destinationPath: string): void

    //sourcePath: string
    copyEditorBackingFilesPreservingPath(sourcePath: string): void

    //sourcePath: string
    //destinationPath: string
    copyEditorBackingFilesWithNewRelativePath(sourcePath: string, destinationPath: string): void

    //sourcePath: string
    //destinationPath: string
    copyFile(sourcePath: string, destinationPath: string): void

    //sourcePath: string
    //destinationPath: string
    copyFileOrFail(sourcePath: string, destinationPath: string): void

    //path: string
    countFilesInDirectory(path: string): number

    //path: string
    deleteDirectory(path: string): void

    //path: string
    deleteFile(path: string): void

    //path: string
    directoryExists(path: string): boolean

    //arg0: string
    editUsing(arg0: string): void

    //arg0: any
    eval(arg0: any): void

    //msg: string
    fail(msg: string): void

    //path: string
    //content: string
    fileContains(path: string, content: string): boolean


    fileCount(): number

    //path: string
    fileExists(path: string): boolean

    //path: string
    //content: string
    fileHasContent(path: string, content: string): boolean


    files(): any[]

    //arg0: string
    messageChannel(arg0: string): void

    //path: string
    moveUnder(path: string): void


    name(): string

    //msg: string
    println(msg: string): void


    projects(): any[]

    //arg0: string
    raiseIssue(arg0: string): void

    //regexp: string
    //replacement: string
    regexpReplace(regexp: string, replacement: string): void

    //literal: string
    //replaceWith: string
    replace(literal: string, replaceWith: string): void

    //literal: string
    //replacement: string
    replaceInPath(literal: string, replacement: string): void

}   // interface Services


/*
 * Spring Boot project
 */
interface SpringBootProject {

    //name: string
    //parentPath: string
    addDirectory(name: string, parentPath: string): void

    //directoryPath: string
    addDirectoryAndIntermediates(directoryPath: string): void

    //path: string
    //content: string
    addFile(path: string, content: string): void

    //pkg: string
    //annotationName: string
    annotateBootApplication(pkg: string, annotationName: string): void


    applicationClassFQN(): string


    applicationClassPackage(): string


    applicationClassSimpleName(): string

    //sourcePath: string
    copyEditorBackingFileOrFail(sourcePath: string): void

    //sourcePath: string
    //destinationPath: string
    copyEditorBackingFileOrFail(sourcePath: string, destinationPath: string): void

    //sourcePath: string
    //destinationPath: string
    copyEditorBackingFilesOrFail(sourcePath: string, destinationPath: string): void

    //sourcePath: string
    copyEditorBackingFilesPreservingPath(sourcePath: string): void

    //sourcePath: string
    //destinationPath: string
    copyEditorBackingFilesWithNewRelativePath(sourcePath: string, destinationPath: string): void

    //sourcePath: string
    //destinationPath: string
    copyFile(sourcePath: string, destinationPath: string): void

    //sourcePath: string
    //destinationPath: string
    copyFileOrFail(sourcePath: string, destinationPath: string): void

    //path: string
    countFilesInDirectory(path: string): number

    //path: string
    deleteDirectory(path: string): void

    //path: string
    deleteFile(path: string): void

    //path: string
    directoryExists(path: string): boolean

    //arg0: any
    eval(arg0: any): void

    //msg: string
    fail(msg: string): void

    //path: string
    //content: string
    fileContains(path: string, content: string): boolean


    fileCount(): number

    //path: string
    fileExists(path: string): boolean

    //path: string
    //content: string
    fileHasContent(path: string, content: string): boolean


    files(): any[]


    isMaven(): boolean


    isSpring(): boolean


    isSpringBoot(): boolean


    javaFileCount(): number

    //path: string
    moveUnder(path: string): void


    name(): string


    packages(): any[]

    //msg: string
    println(msg: string): void


    projects(): any[]

    //regexp: string
    //replacement: string
    regexpReplace(regexp: string, replacement: string): void

    //oldPackage: string
    //newPackage: string
    renamePackage(oldPackage: string, newPackage: string): void

    //literal: string
    //replaceWith: string
    replace(literal: string, replaceWith: string): void

    //literal: string
    //replacement: string
    replaceInPath(literal: string, replacement: string): void

}   // interface SpringBootProject


/*
 * YML file
 */
interface Yml {


    content(): string

    //arg0: any
    eval(arg0: any): void

    //msg: string
    fail(msg: string): void


    filename(): string


    isWellFormed(): boolean


    lineCount(): number


    path(): string

    //msg: string
    println(msg: string): void

    //root: string
    underPath(root: string): boolean

    //arg0: string
    //arg1: string
    updateKey(arg0: string, arg1: string): void

    //name: string
    valueOf(name: string): any

}   // interface Yml


export { Dockerfile }
export { ElmModule }
export { File }
export { Http }
export { JavaClass }
export { JavaProject }
export { JavaSource }
export { Json }
export { Line }
export { PackageJSON }
export { Pom }
export { Project }
export { Properties }
export { Python }
export { PythonRequirements }
export { PythonRequirementsTxt }
export { Replacer }
export { Replacerclj }
export { Services }
export { SpringBootProject }
export { Xml }
export { Yml }
