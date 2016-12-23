# Change Log

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/)
and this project adheres to [Semantic Versioning](http://semver.org/).

## [Unreleased]

[Unreleased]: https://github.com/atomist/rug/compare/0.7.1...HEAD

### Changed

-   **BREAKING** Naming convention for Rug types now UpperCamelCase,
    basically the same as the Scala class/type without the trailing
    Type, MutableView, or TreeNode

-   **BREAKING** Rug tree expression syntax has been changed to more
    closely resemble XPath where possible

### Fixed

-   @any parameter validation regex now works in both Java and
    Javascript

-   Double-quoted strings in Rug DSL are now interpreted similarly to
    Java double-quoted strings

## [0.7.1] - 2016-12-19

[0.7.1]: https://github.com/atomist/rug/compare/0.7.0...0.7.1

### Added

-   Assertions can use 'not'

-   Improve javadoc

### Changed

-   Various microgrammar improvements

-   Update to latest rug-typescript-compiler

## [0.7.0] - 2016-12-16

[0.7.0]: https://github.com/atomist/rug/compare/0.6.0...0.7.0

### Changed

-   Breaking change to the Typescript Rug programming
    model. Decorators are no longer used, the interface signatures
    have changed, and the mechanism for retrieving the
    PathExpressionEngine has changed. See atomist/rug#24 for details.

-   Breaking change to the Message trait. Now required to implement
    withActionNamed

-   Breaking change to message.Rug interface, type is now part of Rug
    not calledback

### Added

-   Event handlers definitions moved into Rug open source project

-   Added new node levels in microgrammar returns

## [0.6.0] - 2016-12-14

[0.6.0]: https://github.com/atomist/rug/compare/0.5.4...0.6.0

### Changed

-   Improved microgrammer tests

-   Fixed error in MatcherMicrogrammar.strictMatch with extra node
    level

-   Update dependencies to latest

-   Only load javascript from .atomist directory

### Added

-   Added Rename for microgrammers

## [0.5.4] - 2016-12-12

[0.5.4]: https://github.com/atomist/rug/compare/0.5.3...0.5.4

Mutually assured success release

### Changed

-   TypeScript Rugs are loaded first, allowing them to be called from
    Rug DSL Rugs

-   All parameter regular expressions must be anchored, i.e., start
    with `^` and end with `$`

-   The name of the Atomist ignore file is now `.atomist/ignore`

-   project.AddFile is now idempotent

### Added

-   Documentation

-   This CHANGELOG

## [0.5.3] - 2016-12-09

The single triple double release

[0.5.3]: https://github.com/atomist/rug/compare/0.5.2...0.5.3

### Fixed

-   Single double-quotes are now properly ignored within triple
    double-quotes

### Changed

-   Timestamped builds are now published to a dev repo
