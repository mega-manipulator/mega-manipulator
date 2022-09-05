package com.github.jensim.megamanipulator.settings.types

@FunctionalInterface
interface HostWithAuth {

    val username: String
    val baseUrl: String

    fun getDefaultHeaders(): List<Pair<String, String>> = emptyList()
    fun getAuthHeaderValue(password: String?): String?
}
