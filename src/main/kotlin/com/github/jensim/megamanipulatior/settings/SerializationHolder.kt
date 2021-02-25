package com.github.jensim.megamanipulatior.settings

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.registerKotlinModule

object SerializationHolder {
    val yamlObjectMapper: ObjectMapper by lazy {
        ObjectMapper(YAMLFactory()).registerKotlinModule().apply {
            enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        }
    }
    val jsonObjectMapper: ObjectMapper by lazy {
        ObjectMapper().registerKotlinModule().apply {
            disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        }
    }
}
