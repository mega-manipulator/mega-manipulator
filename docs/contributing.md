# Contributing

Building this repo in GitHub actions requires access to secrets. So PR-builds are not valid, as they are unable to access the base repo secrets.

Each contributor must therefore fork the repo and set up their own secrets. I've tried to make this selection of tokens as slim as possible, using the defaults from GitHub.

You are required to set up one token however.  
[A token to sourcegraph.](/docs/Access/sourcegraph)  
This token is used to validate the SourcegraphClient.

![](/img/fork_secrets.png)
