# Change Log

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/)
and this project adheres to [Semantic Versioning](http://semver.org/).

## [Unreleased]

[Unreleased]: https://github.com/atomist/rug/compare/0.10.0...HEAD

### Added

-   Support for @parameter TS class field decorators as per
        https://github.com/atomist/rug/issues/229

-   Support for a new TS (JS) Handler programming model as per
    https://github.com/atomist/rug/issues/105

-   Support for Type extensions/TreeNode written in TypeScript as
    per https://github.com/atomist/rug/issues/214

-   Generators are now declared with the `generator` keyword

-   Optional predicates in tree expressions

### Fixed

-   LinkedJsonTreeDeserializer now properly returns string values

-   LinkedJsonTreeDeserializer now works when an already linked object
    is updated

-   TS generators are now passed project name as second argument as
    per TS contract

-   Retain all changes from an editor
    https://github.com/atomist/rug/issues/199

### Changed

-   We now create a new JS rug for each thread for safety.
    https://github.com/atomist/rug/issues/78

-   **BREAKING** all JS based Rugs must export (a la Common-JS) vars implementing
    the associated interfaces. Previously we scanned for all top level vars.

-   **BREAKING** Remove Executor support from Rug DSL as per:
    https://github.com/atomist/rug/issues/206

-   TypeScript editors now return void. Use the new
    `ProjectMutableView` `describeChange` method to add any comments
    about the working of your editor.

-   **BREAKING** CustomizingProjectGenerator was removed from
    ProjectGenerator.ts as it's no longer required, and it's thought
    that it's not being used at all yet.

-   **BREAKING** signature of TypeScript ProjectGenerator.populate()
    changed: parameter `projectName` got removed. Name of the
    generated project can be obtained via `project.name()`.

-   Core.ts is generated and compiled on-the-fly during unit-testing
    so that the build is not dependent on network or later maven
    phases until deployment

### Deprecated

-   The `@generator` has been deprecated in favor of the `generator`
    keyword

## [0.10.0]

[0.10.0]: https://github.com/atomist/rug/compare/0.9.0...0.10.0

### Added

-   Parameter validation regex `@version_range`

-   ScriptExceptions during initial eval of JavaScript now include
    filename

-   EveryPom type curtesy of @justinedelson

### Changed

-   TypeScript/JavaScript should now "export" instances of Rugs inline
    with CommonJS standard.  Hopefully not a breaking change as there
    is limited support for automatically exporting legacy ones.

### Fixed

-   Comments are removed from JS files as they are 'required', and
    relative imports now work correctly when 'export' is used from TS
    or vars are added to the global exports var
    https://github.com/atomist/rug/issues/156

-   Generation of Rug types documentation

-   minLength and maxLength now default to -1 as per Rug DSL
    https://github.com/atomist/rug/issues/169

-   Allow Java annotations with properties
    https://github.com/atomist/rug/issues/164

-   Default parameter values are now validated
    https://github.com/atomist/rug/issues/168

-   Output parameter name when pattern fails to validate
    https://github.com/atomist/rug/issues/58

### Removed

-   In the DSL, parameters no longer accept an array of allowed
    values for validation.

## [0.9.0] - 2017-01-09

[0.9.0]: https://github.com/atomist/rug/compare/0.8.0...0.9.0

TypeScripting release

### Added

-   TypeScript Reviewers

### Fixed

-   TS parameter tags were not being extracted
    https://github.com/atomist/rug/issues/151
-   Parameters for TS editors/generators were defaulting to displayable=false.
    They now default to displayable=true.
    https://github.com/atomist/rug/issues/148

## [0.8.0] - 2017-01-04

[0.8.0]: https://github.com/atomist/rug/compare/0.7.1...0.8.0

Breaking release

### Added

-   NavigationAxis: New AxisSpecifier that allows navigating via a
    property in path expression language

-   Path expression predicates can contain path expressions

### Changed

-   **BREAKING** Naming convention for Rug types now UpperCamelCase,
    basically the same as the Scala class/type without the trailing
    Type, MutableView, or TreeNode

-   **BREAKING** Rug tree expression syntax has been changed to more
    closely resemble XPath where possible

-   **BREAKING** Rug DSL: A Rug can no longer specify its action in JavaScript:
    In this case, use TypeScript or JavaScript.

-   **BREAKING** FileArtifactMutableView is now named FileMutableView

### Fixed

-   @any parameter validation regex now works in both Java and
    Javascript

-   Double-quoted strings in Rug DSL are now interpreted similarly to
    Java double-quoted strings

-   LABEL section in files parsed by Dockerfile type did not handle
    multi-line strings correctly as per https://github.com/atomist/rug/issues/140

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
