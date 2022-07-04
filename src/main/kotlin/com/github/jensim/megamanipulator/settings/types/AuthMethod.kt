package com.github.jensim.megamanipulator.settings.types

import com.fasterxml.jackson.annotation.JsonPropertyDescription

enum class AuthMethod {
    @JsonPropertyDescription("Username and access token combination")
    USERNAME_TOKEN,

    @JsonPropertyDescription("Token without username")
    JUST_TOKEN,
    NONE,
}
