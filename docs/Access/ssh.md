---
sidebar_position: 5
sidebar_label: SSH
---
# SSH driven git

## Generate a ssh key
[Generating an SSH key](https://docs.github.com/articles/generating-an-ssh-key/)

## Make it usable without passphrase

```shell
eval "$(ssh-agent -s)"
ssh-add
```

## Add it to code hosts
* [GitHub](github)
* [BitBucket Server](bitbucket_server)

#### A word on best practices
https://dev.to/josephmidura/how-to-manage-multiple-ssh-key-pairs-1ik
