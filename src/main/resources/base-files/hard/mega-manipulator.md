# Mega Manipulator

One stop shop for making changes, big or small, in a lot of places at once

[Intro Docs](https://mega-manipulator.github.io/docs/intro)

In essence, I've tried to create tooling to do away with all scaling that comes from solving a problem `2..n` times..

Search and replace is something lots of developers are familiar with. 
Some might even have used `sed` & `comby`.
These tools are fantastic, when working in a single repo.

To truly scale `search & replace` we need these 2 words to become aware of all your code.  
How to find the right repos in the thousands? `SourceGraph` is my favourite. In their OSS solution, they offer search capabilities that are simply beyond what hound, or grok, have ever been able to offer.

# Search
In the settings-file (generated here: `./config/mega-manipulator.json`), you can define search hosts.  
A default one is preconfigured for you. And you can append to this list if you have on-prem instances of sourceGraph or hound.
You are able to run your searches just as on your search host, just copy-paste in-between.

# Replace
In the batch-file (generated here: `./config/mega-manipulator.bash`), you can write the change set you want to be applied to each repo.  
When you hit the `Apply`-button, it will execute the script on every cloned repo.

# Version Control
We support `git`.  
And we support `BitBucket server`, `GitHub` & `GitLab`.
If you have changes you want applied to multiple codeHosts, that's not a problem.

# With great power
I take zero responsibility for what you do with this tool, or what this tool does to your system. :-)

Sourcegraph has a Batch Change product, that is offered in their enterprice product, and I totally recommend that you check that out!  
I actually created mega-manipulator "the other path", to one of SG-batchChanges basic design philosophies. 
Containerized. Sounds good to protect the user from messing up.

Mega Manipulator allows you to mess up. Why?  
Because, running in user mode allows you to test your scripts out rapidly in the terminal, install new tools on your system, and then apply the script to a batch of clones, hassle-free.

![Workflow abstract](https://raw.githubusercontent.com/mega-manipulator/mega-manipulator.github.io/docsrc/static/img/mega-manipulator-overall.svg)

