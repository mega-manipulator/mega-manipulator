package com.github.jensim.megamanipulator.settings.types

import com.fasterxml.jackson.annotation.JsonPropertyDescription

const val forkSettingDescription = """Fork settings is used to decide when to fork a repo:
* PLAIN_BRANCH: Will require write access to the repo
* LAZY_FORK: When not permitted to push into origin, attempt fork strategy
* EAGER_FORK: Fork before push, for every repo"""

enum class ForkSetting {
    @JsonPropertyDescription("Will require write access to the repo")
    PLAIN_BRANCH,

    @JsonPropertyDescription("When not permitted to push into origin, attempt fork strategy")
    LAZY_FORK,

    @JsonPropertyDescription("Fork before push, for every repo")
    EAGER_FORK,
}
