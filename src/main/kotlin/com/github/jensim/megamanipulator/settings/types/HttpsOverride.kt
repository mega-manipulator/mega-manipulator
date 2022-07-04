package com.github.jensim.megamanipulator.settings.types

import com.fasterxml.jackson.annotation.JsonPropertyDescription

enum class HttpsOverride {

    @JsonPropertyDescription("Do not validate certificate at all")
    ALLOW_ANYTHING,
}
