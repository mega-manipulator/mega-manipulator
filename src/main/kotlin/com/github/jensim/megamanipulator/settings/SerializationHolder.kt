package com.github.jensim.megamanipulator.settings

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT
import com.fasterxml.jackson.databind.DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT
import com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule

object SerializationHolder {

    fun ObjectMapper.confCompact(): ObjectMapper {
        configure(FAIL_ON_UNKNOWN_PROPERTIES, false)
        configure(ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true)
        configure(ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT, true)
        enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE)
        setDefaultPropertyInclusion(NON_NULL)
        registerModule(KotlinModule.Builder().build())
        registerModule(JavaTimeModule())
        return this
    }

    fun ObjectMapper.confReadable(): ObjectMapper {
        confCompact()
        configure(INDENT_OUTPUT, true)
        setDefaultPropertyInclusion(JsonInclude.Include.NON_DEFAULT)
        return this
    }

    val objectMapper = ObjectMapper().confCompact()
    val readable = ObjectMapper().confReadable()
}
