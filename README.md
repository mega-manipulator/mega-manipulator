# mega-manipulator

# Work in progress

![Build](https://github.com/jensim/mega-manipulator/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/16396.svg)](https://plugins.jetbrains.com/plugin/16396)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/16396.svg)](https://plugins.jetbrains.com/plugin/16396)

<!-- Plugin description -->

# Mega Manipulator

One stop shop for making changes, big or small, in a lot of places at once.
See this as an alternative to sourcegraph campaigns, without the docker layer inbetween you and the changes you want to make.

Use sourcegraph queries to find target repos.
Then a forEach approach is provided for eache of the following actions:
Apply changes (using scripts),
Commit,
Push,
Fork (lazy, eager, never),
PR.

## Search

Connect to any number of search hosts, and queury those for repository hits  
Clone the found repos to local file system, and inspect/plan the change you want to make

## Apply changes

Run scripted changes defined in `mega-manipulator.bash`, or apply changes from the terminal, or manually, whatever fits
you best.

## Draft PRs

The file system structure of the clones link back to the config defined in `mega-manipulator.yml`, so we are able to
draft PRs for all code hosts you have cloned from. It doesn't even have to have been discovered using the same search
host.  
Once you have created your PRs, you are also able to edit them, or decline, in batches.

## Profit

Spend less time making small, or big, changes


<!-- Plugin description end -->

## Installation

- Using IDE built-in plugin system:

  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "
  mega-manipulator"</kbd> >
  <kbd>Install Plugin</kbd>

- Manually:

  Download the [latest release](https://github.com/jensim/mega-manipulator/releases/latest) and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>


---
Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template
