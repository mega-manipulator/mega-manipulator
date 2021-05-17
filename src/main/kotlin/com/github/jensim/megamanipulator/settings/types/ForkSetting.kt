package com.github.jensim.megamanipulator.settings.types

import com.github.ricky12awesome.jss.JsonSchema
import kotlinx.serialization.Serializable

@Serializable
enum class ForkSetting {
    @JsonSchema.Description(["Will require write access to the repo"])
    PLAIN_BRANCH,

    @JsonSchema.Description(["When not permitted to push into origin, attempt fork strategy"])
    LAZY_FORK,

    @JsonSchema.Description(["Fork before push, for every repo"])
    EAGER_FORK,
}
