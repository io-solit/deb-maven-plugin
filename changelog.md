# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/en/1.0.0/)
and this project adheres to [Semantic Versioning](http://semver.org/spec/v2.0.0.html).

## Unreleased
### Added
*   `man` goal to generate roff man pages from markdown
*   `changelog` goal to generate changelog.gz from markdown
*   `data` goal to fill staging directories
*   plexus config for deb packaging
*   `autoDependencies` and `autoPermissions` parameters for `package` goal
### Fixed
*   `package` goal fails if no control or stage directory present
*   `package` goal fails with npe in case of hanging comma in a dependency list.
*   `copyright` and `populate` goals do not override existing files in a stage directory

## [0.5] - 2018-02-20T01:00:00UTC+0500
### Added 
*   `populate` goal to copy dependencies 
*   `copyright` goal to generate copyright
*   `pacakge` to create a deb file
