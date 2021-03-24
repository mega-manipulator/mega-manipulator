package com.github.jensim.megamanipulator.settings

import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.registerKotlinModule

object SerializationHolder {
    val yamlObjectMapper: ObjectMapper by lazy {
        ObjectMapper(YAMLFactory()).customize()
    }

    val jsonObjectMapper: ObjectMapper by lazy {
        ObjectMapper().customize()
    }

    fun ObjectMapper.customize(): ObjectMapper {
        registerKotlinModule()
        disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        setDefaultPropertyInclusion(Include.NON_NULL)
        return this
    }
}
