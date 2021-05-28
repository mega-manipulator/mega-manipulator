<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# mega-manipulator Changelog

## [Unreleased]
### Added

### Changed

### Deprecated

### Removed

### Fixed

### Security
## [0.0.24]
### Added
- GitLab code host
- HttpClient logging

### Changed
- Use GraphQL lib for som gitlab interations and sourcegraph
- Moved docs into the new namespace 'mega-manipulator'
- Added a border to the html table on validate tokens

### Deprecated

### Removed
- Fork name prefix

### Fixed
- Forks using https clone type

### Security

## [0.0.23]
### Added
- Doc links

### Changed
- Reorder search tab

### Deprecated

### Removed

### Fixed

### Security

## [0.0.22]
### Added
- Git over https
- Support etsy/hound search host type
- Support sourcegraph `repo` and `commit` search types

## [0.0.21]
### Added
- Validate access tokens

## [0.0.20]
### Added
- .env-file support for local development
- Integration test

### Changed
- Application internal wiring rebuilt, to allow testing

### Fixed
- Remove branches from GitHub

### Security
## [0.0.19]
### Fixed
- Fixed pagination start index for GitHub client, fetch forked repos without outgoing PRs.
This will eliminate repos being listed twice

### Security

## [0.0.18]
### Added
- Resizable split panes for clones, PRs, & apply

### Changed
- Default select first option for
  - Reword PRs
  - Apply
  - PRs
  - Clones info

## [0.0.17]
### Changed
- Shallow clone repos, for faster speed and less disk space taken

## [0.0.16]
### Added
- Comment on PRs

## [0.0.15]
### Added
- Open selected PRs in browser

### Removed
- No longer support IntelliJ 2020.2

### Fixed
- Fixed Deprecated api usage of NotificationGroup

## [0.0.14]
### Added
- Fuzzy search open PRs

## [0.0.13]
### Added
- Sonar reports and test coverage

### Changed
- Re-patterned the entire source code, to enable unit testing
- Upgrade gradle wrapper to version 7.0

### Fixed
- Serialization of httpClient types
- Functional default values in conf
- Loading class path files

## [0.0.12]
### Added
- Support IntelliJ 2021.1

## [0.0.11]
### Added
- Added a json schema for the mega-manipulator.yml

### Changed
- Added default settings for connecting to sourcegraph.com and github.com
- Switched everything over to use kotlinx serialization from jackson

## [0.0.10]
### Changed
- Update the plugin description to look better in the plugin marketplace

## [0.0.9]
### Added
- Stabilize how PRs from forks are cloned. It's important that they are set up in the same manner as when one clones the base repo and then set up the fork in the initial flow.
### Changed
- Version bumps
- `USERNAME_PASSWORD` login method was renamed `USERNAME_TOKEN`, to better imply recommended usage

### Removed
- Removed the `TOKEN` login method  

## [0.0.6]
### Added
- Initial scaffold created from [IntelliJ Platform Plugin Template](https://github.com/JetBrains/intellij-platform-plugin-template)
