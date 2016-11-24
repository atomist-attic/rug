# Atomist 'rug-lib'

[![Build Status](https://travis-ci.com/atomisthq/rug-lib.svg?token=43qyuBt1idhSyPKtxZ27&branch=master)](https://travis-ci.com/atomisthq/rug-lib)

Rug editor DSL for project operations, with condolences to the
Dude. See https://docs.atomist.com/ for documentation of the Rug
language.

![Dude](https://s-media-cache-ak0.pinimg.com/564x/d3/0d/80/d30d80d37a36c2fac01ed827f3294d52.jpg)

## Developing this service

### Before you get started...

Make sure you've set your Atomist username and email address before
you manipulate this repository.

You can set these details with:

```sh
$ git config user.name "your username"
$ git config user.email "your Atomist.com email address"
```

### Build and publication

The library builds with Maven. For those new to Maven (anyone outside
of a Java community for example) then you can install it usually with
something like on GNU/Linux:

```sh
$ sudo apt-get install maven
```

or this on Mac OS X/macOS:

```sh
$ brew install maven
```

Once you have Maven installed, you can use

```sh
$ mvn install
```

This will publish to the local repo as many other libraries and
services depend on this.

If you'd like to do a quicker build and you're sure there is nothing
new to be tested, then you could do:

```sh
$ mvn install -DskipTests
```

### Releasing a new version

Creating releases is automated in the [Travis CI][travis] build.  All
you need to do is create a tag that looks like
a [semantic version][semver] release version, i.e., a version that
looks like `M.N.P` with no trailing pre-release version or build
metadata.

```sh
$ git tag -a 1.2.3
$ git push origin --tags
```

The message you provide for the annotated tag will provide the
description for the GitHub release.

[travis]:https://travis-ci.com/atomisthq/parameter-lib
[semver]: http://semver.org
