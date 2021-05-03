---
sidebar_position: 2
---

# Sourcegraph search

Sourcegraph search is superior to all others I've used, hands down.  
In honor of that, I've set up a release train of the OSS parts of the product.  
[![OSS Version (latest semver)](https://img.shields.io/docker/v/jensim/sourcegraph-server-oss?sort=semver&label=sourcegraph%20OSS%20version)][docker_hub_oss]  
[![Enterprise Version (latest semver)](https://img.shields.io/docker/v/sourcegraph/server?color=orange&label=sourcegraph%20enterprise%20version&logo=sourcegraph&sort=semver)][docker_sg]

## Transfer search from sourcegraph to plugin
Copy the search from the web to the plugin.  
Same caveats as with `src cli` apply.  
You might need to set the `petternType:structural` if you are not doing a literal search, or case sensitive. These values are set in the url (but with `=` delimiter instead of `:`, if you don't know how to pinpoint naming)

[docker_hub_oss]: https://hub.docker.com/r/jensim/sourcegraph-server-oss/tags?page=1&ordering=last_updated
[docker_sg]: https://hub.docker.com/r/sourcegraph/server
