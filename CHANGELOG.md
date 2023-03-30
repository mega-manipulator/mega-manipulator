<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# mega-manipulator Changelog

## [Unreleased]
### Security
- No longer maintained 😱
- Bump platform version to 2023.1

## [1.0.4]
### Improvement
- Handle GitHub rate limits better, both primary and secondary

### Fixed
- Sparse checkout from locally kept repo
- Repo reset after local copy
- Merge GitHub PRs

## [1.0.3]
### Improvement
- Notify users that there is a new plugin version available

### Fix
- Also fix the coloring (Tables returned the wrong index after being sorted)

## [1.0.2]
### Fix
- Tables returned the wrong index after being sorted

### Security
- Bump the versions

## [1.0.1]
### Improvement
- Sparse checkout is now possible from a locally kept repo
- Upgrade all `Exception#printStackTrace` to use slf4j `Logger` for improved readability in the IDE logs

### Fixed
- Github search pagination was not working properly, so it was limiting search results to 30 repos
- Catch possible null access when rendering table rows

## [1.0.0]
### Improvement
- Default branch name will include timestamp, for unique branch names, and working with multiple batches at the same time

### Security
- Update ktor version

## [0.0.54]
### Added
- Configurable http concurrency for GitHub Code host to tackle slow loading of PRs and also the over-concurrent http requests for fetching forks
- Fetch pull requests from a specific project or repo (GitHub & GitLab)

### Improvement
- Prefill stuff in the pull request load popup
- Password prompt updates status of password without refresh
- Use right-click context menu to interact with tables rather than a truck-load of buttons everywhere

### Fix
- Table select events only trigger once now

## [0.0.53]
### Improvement
- Simplify the settings tab

## [0.0.52]
### Improvement
- Table sorting added

### Fixed
- Highlight colors for tables used to be unpredictable

## [0.0.51]
### Improvement
- Handle short rate limits in github api with delay and retry

### Changed
- Support idea 2022.1, 2022.2 and 2022.3

### Fix
- Username/Password login for SourceGraph was broken, and used to send token as username
- Test usernames might have been a bit mixed up

### Security
- Upgrade kotlin, ktor, graphQL versions, etc

## [0.0.50]
### Added
- More descriptive validation, with better outputs
- Do not use internal APIs for creating the ToolWindow
- Open Mega Manipulator in any project if the user asks for it

## [0.0.49]
### Added
- Apply history
- Rerun scripted changes from history

## [0.0.48]
### Fix
- Run the restore local repo in the correct directory

### Changed
- Support idea 2022.2 and 2022.3

## [0.0.47]
### Fixed
- Settings loading fault tolerance
- Broken scrolling in most tabs
- Fill tab area

## [0.0.46]
### Added
- Keep repos between clones  
Keep and maintain repos in a user defined dir, restore clones from keep directory.  
This is intended to tackle code host clone throttling.

## [0.0.45]
### Added
- Support idea 2022.2

### Fix
- Update deprecated UI builders
  - Token validation output now fits on screen
- The "Set branch button" in the "Clones window" now works as expected (inverted rex validation)
- Closing tabs in the Mega Manipulator tool window is now blocked

## [0.0.44]
### Added
- GitHub search
- Setting to override http logging

### Improvement
- Improve the informativeness of error outputs from the token validations

### Fix
- Relaxed SSL settings got borked with CIO engine, revert to Apache engine
- [Validate PATH on startup](https://github.com/mega-manipulator/mega-manipulator/issues/136)

## [0.0.43]
### Changed
- Breaking change to the json schema, in order to use jackson

### Fix
- Fix the toolwindow initiation
- Fix version f-up

## [0.0.41]
### Added
- Verify with 2022.1

### Removed
- Verify with 2021

### Changed
- Upgraded versions
- Swapped kotlinx serialization for jackson  
As it seemed to result in odd behaviours running the plugin with newer versions

## [0.0.40]
### Added
- Stop running background processes with the Cancel button

## [0.0.39]
### Added
- Force push

### Fixed
- Validation of branch renaming

### Security
- Upgrade gradle version to 7.3

## [0.0.38]
### Removed
- The unused 'err'-block apply outputs  
stdErr is joined with the stdOut since last

## [0.0.37]
### Fixed
- Multiline output, and err redirect from Apply
- Found workaround for mangled PATH

## [0.0.36]
### Added
- Load closed Pull requests

## [0.0.35]
### Added
- UI-component for popping up prefill from history

## [0.0.34]
### Added
- Sparse checkout from search and PR  
  And unison UI between the two

### Changed
- Improving some look and feel

### Fixed
- Able to navigate tabs with broken config

## [0.0.33]
### Changed
- Improved the UI components
  - Declining PRs
  - Password input
  - Show PR authors

## [0.0.32]
### Added
- In the forks tab, show parent/origin-repo of forks (Github & Bitbucket)

### Changed
- Created a general purpose table component for the Search, PR & Fork tabs

### Security
- Upgrade dependency versions

## [0.0.31]
### Added
- Onboarding flow is now able to select tabs moving forward
- Added onboarding to all tabs
- Optional shallow clone

### Changed
- Use plugin manifest for wiring

### Fixed
- Persistence component now actually persists data between sessions
- Fix BitbucketClient
  - Fetch stale forks
  - Delete fork

## [0.0.30]
### Added
- Force recreate base files
- Force open tabs when opening an MM project
- Onboarding for settings window
- Remember search string

### Changed
- New look and feel of dialogs

## [0.0.29]
### Added
- approve, unapprove & merge for GitHub

### Removed
- shallow clone feature, as it messes up the forked repo remotes integration

## [0.0.28]
### Added
- Fetch review PRs
- Added PR actions
  - approving
  - disapproving
  - merging

## [0.0.27]
### Fixed
- Fixed error for accessing classPath-resources with leading slash
- Fix warning for using internal APIs

## [0.0.26]
### Removed
- Removed support for older versions of IntelliJ

## [0.0.24]
### Added
- GitLab code host
- HttpClient logging

### Changed
- Use GraphQL lib for som gitlab interations and sourcegraph
- Moved docs into the new namespace 'mega-manipulator'
- Added a border to the html table on validate tokens

### Removed
- Fork name prefix

### Fixed
- Forks using https clone type

## [0.0.23]
### Added
- Doc links

### Changed
- Reorder search tab

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

## [0.0.19]
### Fixed
- Fixed pagination start index for GitHub client, fetch forked repos without outgoing PRs.
This will eliminate repos being listed twice

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
