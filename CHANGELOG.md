# Change Log

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/)
and this project adheres to [Semantic Versioning](http://semver.org/).

## [Unreleased]

[Unreleased]: https://github.com/atomist/rug/compare/0.5.4...HEAD

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

-   Single double-quotes are now properly ignored withing triple
    double-quotes

### Changed

-   Timestamped builds are now published to a dev repo
