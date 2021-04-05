package com.github.jensim.megamanipulator.settings

import kotlinx.serialization.json.Json

object SerializationHolder {
    val readableJson: Json by lazy {
        Json {
            isLenient = true
            ignoreUnknownKeys = true
            prettyPrint = true
            prettyPrintIndent = "  "
            encodeDefaults = true
        }
    }
}
