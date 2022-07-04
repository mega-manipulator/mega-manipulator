package com.github.jensim.megamanipulator.settings.types

internal fun validateBaseUrl(baseUrl: String) {
    require(baseUrl.startsWith("http://") || baseUrl.startsWith("https://")) {
        "baseUrl must start with http:// or https://"
    }
    listOf<Char>('/', '?', '=', '&').forEach {
        require(!baseUrl.endsWith(it)) {
            "baseUrl must not end in '$it'"
        }
    }
}
