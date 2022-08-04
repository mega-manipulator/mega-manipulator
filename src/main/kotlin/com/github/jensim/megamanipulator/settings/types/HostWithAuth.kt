package com.github.jensim.megamanipulator.settings.types

@FunctionalInterface
interface HostWithAuth {
    fun getAuthHeaderValue(password: String?): String?
}
