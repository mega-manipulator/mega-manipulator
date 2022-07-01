package com.github.jensim.megamanipulator.settings.types

import com.github.ricky12awesome.jss.JsonSchema
import kotlinx.serialization.Serializable

@Serializable
enum class HttpsOverride {

    @JsonSchema.Description(["Do not validate certificate at all"])
    ALLOW_ANYTHING,
}
