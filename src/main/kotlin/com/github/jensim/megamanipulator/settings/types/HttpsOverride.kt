package com.github.jensim.megamanipulator.settings.types

import com.github.ricky12awesome.jss.JsonSchema
import kotlinx.serialization.Serializable

@Serializable
enum class HttpsOverride {
    @JsonSchema.Description(["A self signed cert is expected to have only one level"])
    ALLOW_SELF_SIGNED_CERT,

    @JsonSchema.Description(["Do not validate certificate at all"])
    ALLOW_ANYTHING,
}
