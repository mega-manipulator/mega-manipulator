package com.github.jensim.megamanipulator.settings.types

enum class HttpLoggingLevel {
    ALL,
    HEADERS,
    BODY,
    INFO,
    NONE,
    ;
}

val defaultHttpLoggingLevel = HttpLoggingLevel.ALL
fun HttpLoggingLevel?.orDefault(): HttpLoggingLevel = this ?: defaultHttpLoggingLevel
