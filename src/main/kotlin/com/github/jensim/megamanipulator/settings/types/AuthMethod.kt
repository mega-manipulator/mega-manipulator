package com.github.jensim.megamanipulator.settings.types

import com.github.ricky12awesome.jss.JsonSchema
import kotlinx.serialization.Serializable

@Serializable
enum class AuthMethod {
    @JsonSchema.Description(["Username and access token combination"])
    USERNAME_TOKEN,

    @JsonSchema.Description(["Token without username"])
    JUST_TOKEN,
    NONE,
}
