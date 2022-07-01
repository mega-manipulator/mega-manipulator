package com.github.jensim.megamanipulator.settings

import com.fasterxml.jackson.databind.DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT
import com.fasterxml.jackson.databind.DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT
import com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule

object SerializationHolder {
    val objectMapper = ObjectMapper().apply {
        configure(FAIL_ON_UNKNOWN_PROPERTIES, false)
        configure(ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true)
        configure(ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT, true)
        registerModule(KotlinModule())
    }
}
