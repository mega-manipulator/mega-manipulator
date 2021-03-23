# mega-manipulatior

# Work in progress

![Build](https://github.com/jensim/mega-manipulatior/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/16377.svg)](https://plugins.jetbrains.com/plugin/16377)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/16377.svg)](https://plugins.jetbrains.com/plugin/16377)

## Template ToDo list

- [x] Create a new [IntelliJ Platform Plugin Template][template] project.
- [ ] Verify the [pluginGroup](/gradle.properties), [plugin ID](/src/main/resources/META-INF/plugin.xml)
  and [sources package](/src/main/kotlin).
- [ ] Review the [Legal Agreements](https://plugins.jetbrains.com/docs/marketplace/legal-agreements.html).
- [x] [Publish a plugin manually](https://plugins.jetbrains.com/docs/intellij/publishing-plugin.html?from=IJPluginTemplate)
  for the first time.
- [x] Set the Plugin ID in the above README badges.
- [ ] Set the [Deployment Token](https://plugins.jetbrains.com/docs/marketplace/plugin-upload.html).
- [ ] Click the <kbd>Watch</kbd> button on the top of the [IntelliJ Platform Plugin Template][template] to be notified
  about releases containing new features and fixes.

<!-- Plugin description -->

# Mega Manipulator

One stop shop for making changes, big or small, in a lot of places at once

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
  mega-manipulatior"</kbd> >
  <kbd>Install Plugin</kbd>

- Manually:

  Download the [latest release](https://github.com/jensim/mega-manipulatior/releases/latest) and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>


---
Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template
