# Atomist 'rug'

[![Build Status](https://travis-ci.org/atomist/rug.svg?branch=master)](https://travis-ci.org/atomist/rug)
[![Slack Status](https://join.atomist.com/badge.svg)](https://join.atomist.com/)

The Rug runtime: Support for Atomist **project operations** and **handlers** (aka *rugs*). Rugs are authored in JavaScript or any language that compiles to JavaScript capable of executing in [Nashorn](https://en.wikipedia.org/wiki/Nashorn_(JavaScript_engine)). We recommend [TypeScript](http://www.typescriptlang.org/) and provide TypeScript interfaces for the Atomist project and team model.

Key areas of functionality of this project:


|  Area |  Purpose | Base Package/Path |  Remarks |
|---|---|---|---|---|
|  Tree model | Models project and file structure (e.g. ASTs) as a unified tree  | `com.atomist.tree`  | Used by parser and path expressions
| Project operation support  | Editors and executors  |   `com.atomist.project`|   Project operations are authored in TypeScript, JavaScript or any language that compiles to JavaScript.
|  Parsing support |  Support for parsing files and preserving positional information to allow clean in-place updates | `com.atomist.tree.content.text`   | Integrates with Scala parser combinators and Antlr. *Microgrammar support in early development.*   |
| JavaScript/TypeScript integration | Allows project operations to be written in JavaScript/TypeScript | `com.atomist.rug.runtime.js` | Uses Nashorn
| Path expression language | XPath-like language for conveniently navigating trees | `com.atomist.tree` | Incomplete, but an important part of the ultimate vision
| TypeScript library |A `node` module to simplify authoring TypeScript rugs  | `src/main/typescript`| See [architectural overview](https://github.com/atomist/rug/blob/master/docs/TypeScriptLibrary.md). Will eventually be moved into a separate project.

See

*  [Documentation](http://docs.atomist.com/).
*  [Introductory blog](https://medium.com/the-composition/software-that-writes-and-evolves-software-953578a6fc36#.blgtxoyu4) by Rod Johnson

## Using

Most users will not need to use this project directly, but will use
tools, e.g., [rug-cli][cli], that build on this project.

[cli]: https://github.com/atomist/rug-cli

If you wish to develop tools using this project, you will need to add
this project as a dependency and the maven repository where it is
published to your build tool's configuration.  For example, if you use
maven, add the dependency to the `<dependencies>` section and the
repository to the `<repositories>` section of your `pom.xml`:

```xml
<?xml version='1.0' encoding='UTF-8'?>
<project
	xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd"
	xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns="http://maven.apache.org/POM/4.0.0">
	<modelVersion>4.0.0</modelVersion>
    ...
    <dependencies>
        ...
		<dependency>
			<groupId>com.atomist</groupId>
			<artifactId>rug</artifactId>
			<version>0.3.2</version>
			<exclusions>
				<exclusion>
					<groupId>ch.qos.logback</groupId>
					<artifactId>logback-classic</artifactId>
				</exclusion>
				<exclusion>
					<groupId>ch.qos.logback</groupId>
					<artifactId>logback-access</artifactId>
				</exclusion>
			</exclusions>
		</dependency>
        ...
	</dependencies>
    ...
	<repositories>
		<repository>
			<id>public-atomist-release</id>
			<name>Atomist Release</name>
			<url>https://atomist.jfrog.io/atomist/libs-release</url>
			<snapshots>
				<enabled>false</enabled>
			</snapshots>
		</repository>
	</repositories>
    ...
</project>
```

Be sure to change the `<version>` to the one you want to use.

## Support

General support questions should be discussed in the `#support`
channel on our community slack team
at [atomist-community.slack.com](https://join.atomist.com).

If you find a problem, please create an [issue][].

[issue]: https://github.com/atomist/rug/issues

## Development

You can build, test, and install the project locally with [maven][].

[maven]: https://maven.apache.org/

```
$ mvn install
```

To create a new release of the project, simply push a tag of the form
`M.N.P` where `M`, `N`, and `P` are integers that form the next
appropriate [semantic version][semver] for release.  For example:

```
$ git tag -a 1.2.3
```

The Travis CI build (see badge at the top of this page) will
automatically create a GitHub release using the tag name for the
release and the comment provided on the annotated tag as the contents
of the release notes.  It will also automatically upload the needed
artifacts.

[semver]: http://semver.org

The Rug extension documentation is created as part of running the
Maven lifecycle `test` phase under the `npm-release` profile.

```
$ mvn -P npm-release test
```

The documentation for all of the Rug extensions will be in a directory
named `target/.atomist/node_modules/@atomist/rug/typedoc`.
