# Mega Manipulator

One stop shop for making changes, big or small, in a lot of places at once

## How to

* Search
* Apply changes
* Draft PRs
* Profit

## ToDo's

#### 1. Validate config

* [x] Validate configuration, and give user feedback
* [ ] Create special project type not to sully intellij in other use-cases

#### 2. Search

* [x] Search for repos using SourceGraph
* [x] Allow multiple search hosts
* [ ] Search for repos using etsy/hound
* [x] Clone repos
* [x] Select a branchName for all cloned repos and switch to that branch

#### 3. Apply changes

* [x] Write a scripted change-set in `mega-manipulator.bash` and have it applied to all cloned repos
* [x] Split output from each repo
* [x] Color by exit code

#### 4. Commit & Push

* [x] Commit
* [ ] Push
* [x] Push to forks
* [ ] Lazy create forks
* [ ] Eager create forks
* [ ] Cross code hosts

#### 5. Create PRs

* [x] Define a title and body for all the branches, and lean back as we
* [x] Cross code hosts

#### 6. Manage PRs

* [x] List PRs
* [x] Reword a bunch of PRs at the same time
* [x] Add reviewers
* [x] Remove PRs
    * [ ] Drop forks
    * [ ] Drop branches
* [ ] Clone from PRs
    * [ ] set up forked remotes
* [ ] Drop stale forked repos
* [ ] 

#### 7. CodeHosts

* [x] BitBucket Server
* [ ] GitHub.com
* [ ] GitLab
* [ ] ...
