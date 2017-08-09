# Atomist 'rug'

[![Build Status](https://travis-ci.org/atomist/rug.svg?branch=master)](https://travis-ci.org/atomist/rug)

The Rug runtime: Support for Atomist **project operations** and **handlers** (aka *rugs*). Rugs are authored in JavaScript or any language that compiles to JavaScript capable of executing in [Nashorn](https://en.wikipedia.org/wiki/Nashorn_(JavaScript_engine)). We recommend [TypeScript](http://www.typescriptlang.org/) and provide TypeScript interfaces for the Atomist project and team model.

Key areas of functionality of this project:


|  Area |  Purpose | Base Package/Path |  Remarks |
|---|---|---|---|
|  Tree model | Models project and file structure (e.g. ASTs) as a unified tree  | `com.atomist.tree`  | Used by parser and path expressions
| Project operation support  | Editors and executors  |   `com.atomist.project`|   Project operations are authored in TypeScript, JavaScript or any language that compiles to JavaScript.
|  Parsing support |  Support for parsing files and preserving positional information to allow clean in-place updates | `com.atomist.tree.content.text`   | Integrates with Scala parser combinators and Antlr. 
| JavaScript/TypeScript integration | Allows project operations to be written in JavaScript/TypeScript | `com.atomist.rug.runtime.js` | Uses Nashorn
| Path expression language | XPath-like language for conveniently navigating trees | `com.atomist.tree` | Central concept for navigating project and model structure
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

You can build, test, and install the project locally
with [Maven][maven].

[maven]: https://maven.apache.org/

```
$ mvn install
```

This will build, test, and locally install the Rug library.  To build
the Rug TypeScript `@atomist/rug` and `@atomist/cortex` modules, the
"npm-release" profile must be used.  Unlike the [cortex][] build, the
cortex model is not downloaded dynamically because Rug tracks the
latest, perhaps unreleased, version of the model.

To build the TypeScript modules and their documentation, generated
from the TypeScript modules using [TypeDoc][typedoc], execute at least
through the Maven lifecycle `test` phase using the `npm-release`
profile.

```
$ mvn -P npm-release test
```

The documentation for all of the Rug extensions will be in a directory
named `target/typedoc`.

Development versions of the `@atomist/rug` npm module are published to
`https://atomist.jfrog.io/atomist/api/npm/npm-dev`. The most
straightforward way to get these versions without making changes to
your configuration is:

```bash
$ npm install @atomist/rug --registry https://atomist.jfrog.io/atomist/api/npm/npm-dev
```

Alternatively, if you always want the latest snapshot, you can change
your config for the @atomist scope:

```
npm config set @atomist:registry https://atomist.jfrog.io/atomist/api/npm/npm-dev
```

[typedoc]: http://typedoc.org/

## Release

Releasing Rug involves releasing the JVM artifacts to a Maven
repository, the TypeScript module to NPM, and the documentation to
GitHub pages for this repository, available at
http://apidocs.atomist.com/ .  Releasing Rug can be a multi-stepped
process, depending on the changes that have been made.

If there are no changes to the TypeScript API, to create a release
simply push a tag of the form `M.N.P` where `M`, `N`, and `P` are
integers that form the next appropriate [semantic version][semver] for
release.  For example:

```
$ git tag -a 1.2.3
```

The Travis CI build (see badge at the top of this page) will upload
the needed artifacts and automatically create a GitHub release using
the tag name for the release and the comment provided on the annotated
tag as the contents of the release notes.  The released artifacts are:

-   Maven rug artifacts
-   [`@atomist/rug`][rug-npm] Node module to [NPM][npm]
-   TypeDoc for `@atomist/rug` and [`@atomist/cortex`][cortex-npm] to
    the gh-pages branch of this repository, available under
    http://apidocs.atomist.com/typedoc/

Note that while the `@atomist/cortex` module is built as part of the
rug build, it is published by the [cortex][]
repository [build][cortex-build].  The cortex release process also
publishes the cortex TypeDoc to http://cortex.atomist.com .  So if the
API of cortex has changed at all, you will need to initiate a release
of that repository.  This confusion will need to be resolved at some
point in the future.

[semver]: http://semver.org
[rug-npm]: https://www.npmjs.com/package/@atomist/rug
[npm]: https://www.npmjs.com/
[cortex-npm]: https://www.npmjs.com/package/@atomist/cortex
[cortex]: https://github.com/atomist/cortex
[cortex-build]: https://travis-ci.org/atomist/cortex

If there have been changes to either the `@atomist/rug` or
`@atomist/cortex` TypeScript APIs, you will need to update the
dependencies in the [rugs][] repository `package.json` and initiate a
new release of that repository to publish a new version of the
`@atomist/rugs` TypeScript module, which is the dependency everyone
uses to bring in rug and cortex.  This should all be automated,
perhaps in a separate repository or by chaining events to span
repositories.

[rugs]: https://github.com/atomist/rugs

---
Created by [Atomist][atomist].
Need Help?  [Join our Slack team][slack].

[atomist]: https://www.atomist.com/
[slack]: https://join.atomist.com/
