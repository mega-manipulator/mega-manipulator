package com.github.jensim.megamanipulator.settings.types

import com.fasterxml.jackson.annotation.JsonPropertyDescription

enum class HttpsOverride {

    @JsonPropertyDescription("Allow self signed certificates")
    ALLOW_SELF_SIGNED,
    @JsonPropertyDescription("Do not validate certificate at all")
    ALLOW_ANYTHING,
}
