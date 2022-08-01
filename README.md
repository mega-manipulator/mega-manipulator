# mega-manipulator

[![Build](https://github.com/mega-manipulator/mega-manipulator/workflows/Build/badge.svg)](https://github.com/mega-manipulator/mega-manipulator/actions/workflows/build.yml)
![GitHub release (latest by date)](https://img.shields.io/github/v/release/mega-manipulator/mega-manipulator?label=GitHub%20Release&logo=GitHub)
[![Version](https://img.shields.io/jetbrains/plugin/v/16396.svg)](https://plugins.jetbrains.com/plugin/16396)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/16396.svg)](https://plugins.jetbrains.com/plugin/16396)

<!-- Plugin description -->

<h1><b>Mega Manipulator (m²)</b></h1>

<p>
One stop shop for making changes, big or small, in a lot of places at once.
See this as an alternative to sourcegraph campaigns, without the docker layer in between you, and the changes you want to make.
</p>

<p>
Use sourcegraph queries to find target repos.<br>
Then a forEach approach is provided for each of the following actions:
<ul>
<li>Apply changes 
  <ul>
    <li>InteliJ Search & Replace</li>
    <li>Using script</li>
    <li>Direct terminal access</li>
  </ul>
</li>
<li>Commit</li>
<li>Push</li>
<li>Fork (lazy, eager, never)</li>
<li>PR
<ul>
  <li>Create</li>
  <li>Edit</li>
  <li>Approve</li>
  <li>Comment</li>
  <li>Merge</li>
  <li>Close</li>
  <li>Clone</li>
</ul></li>
</ul>
</p>

<h2><b>Search</b></h2>

Connect to any number of search hosts, and queury those for repository hits  
Clone the found repos to local file system, and inspect/plan the change you want to make

<h2><b>Apply changes</b></h2>

Run scripted changes defined in `mega-manipulator.bash`, or apply changes from the terminal, or manually, whatever fits
you best.

<h2><b>Draft PRs</b></h2>

The file system structure of the clones link back to the config defined in `mega-manipulator.yml`, so we are able to
draft PRs for all code hosts you have cloned from. It doesn't even have to have been discovered using the same search
host.  
Once you have created your PRs, you are also able to edit them, or decline, in batches.

<h2><b>Profit</b></h2>

Spend less time making small, or big, changes


<!-- Plugin description end -->

## Installation

- Using IDE built-in plugin system:

  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "mega-manipulator"</kbd> > <kbd>Install Plugin</kbd>

- Manually:

  Download the [latest release](https://github.com/mega-manipulator/mega-manipulator/releases/latest) and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

## Contribution

### Tests

To run tests you have to set some environment variables:  
`GITHUB_USERNAME` and `GITHUB_TOKEN` you can do it creating `.env` file in the project root path or setting it in the environment variable.  
Take a look in `.env-example`, or copy it to `.env`.

---
Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template
