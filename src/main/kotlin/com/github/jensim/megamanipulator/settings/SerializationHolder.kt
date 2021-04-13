package com.github.jensim.megamanipulator.settings

import kotlinx.serialization.json.Json

class SerializationHolder {

    companion object {
        val instance by lazy { SerializationHolder() }
    }

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
