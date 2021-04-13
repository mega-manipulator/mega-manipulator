<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# mega-manipulator Changelog

## [Unreleased]
### Added

### Changed
- Re-patterned the entire source code, to enable unit testing
- Upgrade gradle wrapper to version 7.0

### Deprecated

### Removed

### Fixed
- Serialization of httpClient types
- Functional default values in conf
- Loading class path files

### Security
## [0.0.12]
### Added
- Support IntelliJ 2021.1

### Changed

### Deprecated

### Removed

### Fixed

### Security
## [0.0.11]
### Added
- Added a json schema for the mega-manipulator.yml

### Changed
- Added default settings for connecting to sourcegraph.com and github.com
- Switched everything over to use kotlinx serialization from jackson

### Deprecated

### Removed

### Fixed

### Security
## [0.0.10]
### Added

### Changed
- Update the plugin description to look better in the plugin marketplace

### Deprecated

### Removed

### Fixed

### Security
## [0.0.9]
### Added
- Stabilize how PRs from forks are cloned. It's important that they are set up in the same manner as when one clones the base repo and then set up the fork in the initial flow.
### Changed
- Version bumps
- `USERNAME_PASSWORD` login method was renamed `USERNAME_TOKEN`, to better imply recommended usage

### Deprecated

### Removed
- Removed the `TOKEN` login method  

### Fixed

### Security
## [0.0.6]
### Added
- Initial scaffold created from [IntelliJ Platform Plugin Template](https://github.com/JetBrains/intellij-platform-plugin-template)
