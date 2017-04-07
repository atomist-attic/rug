# Change Log

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/)
and this project adheres to [Semantic Versioning](http://semver.org/).

## [Unreleased]

[Unreleased]: https://github.com/atomist/rug/compare/0.24.0...HEAD

## Changed

-   **BREAKING** Generated TypeScript project model types use
    properties rather than functions for no-arg operations marked with
    `@ExportFunction` when the new `exposeAsProperty` flag is true
-   **BREAKING** All Generated TypeScript Cortex types use properties
    rather than functions for all navigation
-   **BREAKING** Use specific message types in Plans for the different
    scenarios (directed, lifecycle and response) as per [#501][501]
-   Improved build
-   Make TypeScript generator for rug more like that for cortex
-   Improve release documentation

[501]: https://github.com/atomist/rug/issues/501

## [0.24.0] - 2017-04-04

[0.24.0]: https://github.com/atomist/rug/compare/0.23.0...0.24.0

The one TS to rule all stubs

### Changed

-   Model stubs are generated in one uber file
-   Generally refined TS generation

### Added

-   Set the channel id on a message with `withChannelId`

## [0.23.0] - 2017-03-31

[0.23.0]: https://github.com/atomist/rug/compare/0.22.0...0.23.0

TypeScript stub generation and proxy improvement release

### Changed

-   Now generating `add` instead of `with` methods to handle arrays in
    TypeScript code generation

## [0.22.0] - 2017-03-31


### Changed

-   **BREAKING** internal API for resolving and loading Rugs has changed to deal with
    more complex dependency resolution scenarios. Also removal of AddressableRug and
    RugSupport traits.

[0.22.0]: https://github.com/atomist/rug/compare/0.21.0...0.22.0

Back from the future release

### Changed

-   Removed use of `Futures` in `LocalPlanRunner`
-   Changed `ServiceLoader`-based registries to not use caching
-  If the fixture is the same as the world in a Gherkin step, pass only one parameter. This changes the
signatures of handler tests.

## [0.21.0] - 2017-03-30

[0.21.0]: https://github.com/atomist/rug/compare/0.20.0...0.21.0

Handler return release

### Changed

-  **BREAKING** Handlers must return `Plan`, not `Message`
-  **BREAKING** Messages can only contain a string value, not JSON
-  Cardinality is processed in `TreeNode` deserialization from Cortex ingestion

## [0.20.0] - 2017-03-29

[0.20.0]: https://github.com/atomist/rug/compare/0.19.0...0.20.0

Handler testing improvement release

### Added

-   Added defaultValue parameter to @Parameter annotation

### Changed

-   Publishing events on path expression evaluations in handler tests
-   Now pass back JavaScript Plan to Gherkin steps
-   Can now record more than one plan in Gherkin handler tests. Fixes #480
-   TypeScript Plan class now exposes properties rather than functions
-   Do not include TypeDoc in NPM module

## [0.19.0] - 2017-03-28

[0.19.0]: https://github.com/atomist/rug/compare/0.18.3...0.19.0

### Added

-   Added tag to commit back relationship
-   Added platform property on builds
-   Added new methods for pluginManagement

### Changed

-   Better error message when failing to navigate property
-   Better error message when handler was not registered in a test
-   Building typedocs for cortex as well as rug

## [0.18.3] - 2017-03-27

[0.18.3]: https://github.com/atomist/rug/compare/0.18.2...0.18.3

Minor path expression fix release

## [0.18.2] - 2017-03-27

[0.18.2]: https://github.com/atomist/rug/compare/0.18.1...0.18.2

Re-Re-Cortex release

### Fixed

-   NPM publish of cortex

## [0.18.1] - 2017-03-27

[0.18.1]: https://github.com/atomist/rug/compare/0.18.0...0.18.1

Re-Cortex release

### Fixed

-   Cortex package.json

### Added

-   Generate and publish cortex TypeDoc

## [0.18.0] - 2017-03-27

[0.18.0]: https://github.com/atomist/rug/compare/0.17.3...0.18.0

Cortex release

### Added

-   Add placeholder npm module @atomist/native for pure
    TypeScript/JavaScript helpers that have no reliance on JVM objects
    (to be split out later)
-   Support for generation of TypeScript interfaces and stub classes
    for Atomist "cortex" model around commits, builds etc. Lives in
    @atomist/cortex module.

### Changed

-   Travis CI build always uses npm-release Maven profile

## [0.17.3] - 2017-03-24

[0.17.3]: https://github.com/atomist/rug/compare/0.17.2...0.17.3

Fixup before prod rollout release

### Fixed

-   'null' and undefined are not serialized to JSON in instruction
    parameters [#466][466]
-   NodesWithTag allows dynamic nodes on `Child` axis to not match
    without error
-   Don't set default parameter values if they are null [#458][458]
-   Support optional parameters in AnnotatedRugFunctions [#462][462]

[466]: https://github.com/atomist/rug/issues/466
[458]: https://github.com/atomist/rug/issues/458
[462]: https://github.com/atomist/rug/issues/462

## [0.17.2] - 2017-03-22

[0.17.2]: https://github.com/atomist/rug/compare/0.17.1...0.17.2

Quiet release

### Added

-   Allow type safe creation of plan instructions using instantiated operations
-   Allow construction of Message without body

### Fixed

-   Ensure Event Handlers are not invoked if there are no PE matches
    https://github.com/atomist/rug/issues/454

### Added

-   A message method on `HandlerScenarioWorld` when an handler only returns a
    message without a plan

## [0.17.1] - 2017-03-20

[0.17.1]: https://github.com/atomist/rug/compare/0.17.0...0.17.1

Relaxed release

### Changed

-   Removed validation of secret paths as this contract is owned by the bot

## [0.17.0] - 2017-03-20

[0.17.0]: https://github.com/atomist/rug/compare/0.16.0...0.17.0

Handler cleanup release

### Added

-   Added support for parameterized Gherkin steps, per #431

### Changed

-   **BREAKING** MappedParameter names changed to be more specific
-   **BREAKING** changed `withTreeNode` to `withNode` in Message class in Handlers.ts

## [0.16.0] - 2017-03-19

[0.16.0]: https://github.com/atomist/rug/compare/0.15.1...0.16.0

Test good parameters release

### Added

-   Testing well-known step "parameter were valid"

## [0.15.1] - 2017-03-19

[0.15.1]: https://github.com/atomist/rug/compare/0.15.0...0.15.1

Test bad parameters release

### Fixed

-   Testing editors no longer crashes when provided invalid
    parameters [#444][444]

[444]: https://github.com/atomist/rug/issues/444

### Added

-   Query by example option for authoring path expressions against model

## [0.15.0] - 2017-03-17

[0.15.0]: https://github.com/atomist/rug/compare/0.14.0...0.15.0

Early often release

### Added

-   Next phase of handler testing support
-   Adds the concept of a contextRoot node to RugContext to enable
    handlers to run path expressions as needed.  This change impacts
    runtime as well as testing, but is necessary to support our
    programming model.
-   Gherkin testing "the archive root" Given
-   TypeScript test documentation to generated TypeDoc

### Changed

-   **BREAKING** Removed `TreeHelper` class. This module now contains only
    functions, as a class was not necessary.
-   **BREAKING** Python Requirements old type has been removed as it
    was not matching the current programming model.  It will be moved
    to its own project.  [#434][434]
-   Improve duplicate removal in expression engine
-   Make TypeScript test stubs addressable (to help with deduping)
-   Correct some usages of TreeNode where GraphNode could be used
-   **BREAKING** The `generateWith` method used in testing has an
    additional `projectName` parameter between the generator and its
    parameters.

[434]: https://github.com/atomist/rug/pull/434

### Fixed

-   Editor test project objects have a name [#436][436]

[436]: https://github.com/atomist/rug/issues/436

## [0.14.0] - 2017-03-15

[0.14.0]: https://github.com/atomist/rug/compare/0.13.0...0.14.0

Ides of March release

### Changed

-   TypeDoc, ScalaDoc, and scoverage reports are now published
    automatically
-   Xml extension can now derive itself from a file
-   `RugContext` now exposes a `contextRoot` to allow handlers to
     execute arbitrary path expressions
-   Generators should now be under `.atomist/generators` but are still
    found under `.atomist/editors`

### Added

-   Command and event handler testing
-   TypeScript "it should fail" test step via @justinedelson #429
-   Xml `addNodeIfNotPresent` via @justinedelson #423

### Fixed

-   TypeScript interface and class generation #427

## [0.13.0] - 2017-03-10

[0.13.0]: https://github.com/atomist/rug/compare/0.12.0...0.13.0

Unlucky release

### Added

-   Annotations and helper class for implementing RugFunctions
-   New BDD test framework for Rug archives, based on Gherkin and
    TypeScript/JavaScript
-   Support for replacing tokens with secrets in RugFunction parameters
-   Support for resolving secrets during Plan execution
-   Support for MappedParameters to pull config from other systems
-   Support for Secrets in RugCommands
-   Support for Command, Event and Response Handlers. All return Plans
-   Support for RugFunctions - parameterized JVM functions for Handlers
-   Directory extension now public #409

### Fixed

-   Referencing of editors in other archives in JS archives
-   Bug in LinkedJsonTreeDeserializer where it threw NPE on empty
    result set from neo4j-ingester e.g. where the PE didn't match. Now
    returns EmptyLinkableContainerTreeNode

### Changed

-   Upgraded TypeScript to 2.2.1 via rug-typescript-compiler 0.12.0
-   **BREAKING** Removed Rug DSL-based BDD support. Use new Gherkin-based support.
-   **BREAKING** Removed rug namespace support in preference to common
    approach of group:artifact:name This means that DSL based rugs no
    longer contain their namepsace in their name
-   **BREAKING** Only require context when running handlers
-   **BREAKING** Removed CommandRegistry and related usages
-   **BREAKING** Renamed old RugFunction to RugDslFunction
-   New text node structure. See [docs/treenodes.md]().
-   **BREAKING** Removed ProjectOperationArguments. Use ParameterizedRug instead
-   **BREAKING** Refactored loading of Rugs from archives. Now returns `Rugs`
-   **BREAKING** Removed Executor and old Handlers completely

## [0.12.0] - 2017-02-14

[0.12.0]: https://github.com/atomist/rug/compare/0.11.0...0.12.0

Valentine release

### Added

-   Added YamlType and YamlFileType classes to replace YmlType and
    YmlFileType respectively. Deprecated the latter classes.
-   Support PathExpressions that start with a NodesWithTag
-   Support for `contains` and `starts-with` path expression functions
-   Beginning of a TypeScript class library to simplify working with
    tree nodes and transparently decorate tree nodes if a decoration
    strategy is provided in TypeScript.
-   Add new minimal TS interfaces for use with class
    decorators. e.g. EditProject
-   Added support for @Editor, @Generator, @Reviewer and @Tags class
    decorators in TS.
-   Added `ScalaFileType` backed by ScalaMeta

### Fixed

-   Elm parser failed on files with two multiline comments [#268][268]
-   Raise an `InvalidRugTestScenarioName` when a Rug test scenario is
    missing a name #71
-   Implicit DLS parameters (from `uses`) are no longer duplicate if
    multiple editors declare the same parameter. The first one is
    chosen [#258][258]
-   Ensure TS parameters are required by default, and ensure defaults
    are applied before validation [#224][224]
-   Changed how descendants were found and processed in tree
    expressions which fixed two pendingUntilFixed tests

[268]: https://github.com/atomist/rug/issues/268
[258]: https://github.com/atomist/rug/issues/258
[224]: https://github.com/atomist/rug/issues/224

### Changed

-   Upgrade TS compiler to 2.1.5
-   `scalar` method on TypeScript `PathExpressionEngine` taking string
    is now called `scalarStr`. This change was necessary to avoid
    conflicts with method overloading in TypeScript/JavaScript.
-   `TreeNodeUtils.toShorterString` now optionally takes a function
    to customize the display of each node.
-   **BREAKING** `value`, `update` and `formatInfo` functions from
    TypeScript `TreeNode` moved to new `TextTreeNode` sub-interface.
-   **BREAKING** The interface for Rug extensions, formerly Rug types,
    was simplified.  resolvesFromNodeTypes was eliminated and the
    signature for findAllIn was simplified.  They should also directly
    extend ChildResolver instead of some sort of ViewFinder.

## [0.11.0] - 2017-02-01

[0.11.0]: https://github.com/atomist/rug/compare/0.10.0...0.11.0

### Added

-   Implement JsonBackedContainerTreeNode, allows a ContainerTreeNode to
    maintain the Json is was generated from
-   Add correlationId to Message, allows handler to define
    how Messages are correlated
-   Support for @parameter TS class field decorators as
    per [#229][229]
-   Support for a new TS (JS) Handler programming model as
    per [#105][105]
-   Support for Type extensions/TreeNode written in TypeScript as
    per [#214][214]
-   Generators are now declared with the `generator` keyword
-   Optional predicates in tree expressions

[229]: https://github.com/atomist/rug/issues/229
[105]: https://github.com/atomist/rug/issues/105
[214]: https://github.com/atomist/rug/issues/214

### Fixed

-   LinkedJsonTreeDeserializer now properly returns string values
-   LinkedJsonTreeDeserializer now works when an already linked object
    is updated
-   TS generators are now passed project name as second argument as
    per TS contract
-   Retain all changes from an editor [#199][199]
-   Yml type can now be instantiated from ProjectMutableView,
    DirectoryMutableView, and FileMutableView [#250][250]
-   Handle YAML files with multiple documents, but only first is
    parsed and addressable.

[199]: https://github.com/atomist/rug/issues/199
[250]: https://github.com/atomist/rug/issues/250

### Changed

-   **BREAKING** `TreeNode.nodeType` renamed to `TreeNode.nodeTags`
-   We now create a new JS rug for each thread for safety [#78][78]
-   **BREAKING** all JS based Rugs must export (a la Common-JS) vars implementing
    the associated interfaces. Previously we scanned for all top level vars.
-   **BREAKING** Remove Executor support from Rug DSL as per [#206][206]
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
-   Scala compiler warnings are now fatal by default, use `-P
    allow-warnings` profile to have the old behavior.  The Travis CI
    build uses the allow-warnings profile.

[78]: https://github.com/atomist/rug/issues/78
[206]: https://github.com/atomist/rug/issues/206

### Deprecated

-   The `@generator` has been deprecated in favor of the `generator`
    keyword

## [0.10.0] - 2017-01-18

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
    or vars are added to the global exports var [#156][156]
-   Generation of Rug types documentation
-   minLength and maxLength now default to -1 as per Rug
    DSL [#169][169]
-   Allow Java annotations with properties [#164][164]
-   Default parameter values are now validated [#168][168]
-   Output parameter name when pattern fails to validate [#58][58]

[156]: https://github.com/atomist/rug/issues/156
[169]: https://github.com/atomist/rug/issues/169
[164]: https://github.com/atomist/rug/issues/164
[168]: https://github.com/atomist/rug/issues/168
[58]: https://github.com/atomist/rug/issues/58

### Removed

-   In the DSL, parameters no longer accept an array of allowed
    values for validation.

## [0.9.0] - 2017-01-09

[0.9.0]: https://github.com/atomist/rug/compare/0.8.0...0.9.0

TypeScripting release

### Added

-   TypeScript Reviewers

### Fixed

-   TS parameter tags were not being extracted [#151][151]
-   Parameters for TS editors/generators were defaulting to
    displayable=false.  They now default to
    displayable=true.  [#148][148]

[151]: https://github.com/atomist/rug/issues/151
[148]: https://github.com/atomist/rug/issues/148

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
    multi-line strings correctly as per [#140][140]

[140]: https://github.com/atomist/rug/issues/140

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
