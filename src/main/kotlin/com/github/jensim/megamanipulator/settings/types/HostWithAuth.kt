package com.github.jensim.megamanipulator.settings.types

import com.fasterxml.jackson.annotation.JsonIgnore

@FunctionalInterface
interface HostWithAuth {

    val username: String
    val baseUrl: String

    @JsonIgnore
    fun getDefaultHeaders(): List<Pair<String, String>> = emptyList()
    @JsonIgnore
    fun getAuthHeaderValue(password: String?): String?
}
