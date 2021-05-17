package com.github.jensim.megamanipulator.settings.types

interface HostWithAuth {
    fun getAuthHeaderValue(password: String?): String?
}
