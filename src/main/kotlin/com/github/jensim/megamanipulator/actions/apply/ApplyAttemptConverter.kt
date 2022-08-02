package com.github.jensim.megamanipulator.actions.apply

import com.github.jensim.megamanipulator.settings.SerializationHolder
import com.intellij.util.xmlb.Converter
import org.slf4j.LoggerFactory

class ApplyAttemptConverter : Converter<MutableList<ApplyAttempt>>() {

    companion object {
        private val logger = LoggerFactory.getLogger(ApplyAttempt::class.java)
    }

    override fun toString(value: MutableList<ApplyAttempt>): String? {
        return SerializationHolder.objectMapper.writeValueAsString(value)
    }

    override fun fromString(value: String): MutableList<ApplyAttempt>? = try {
        val arr: Array<ApplyAttempt> = SerializationHolder.objectMapper
            .readValue(value, Array<ApplyAttempt>::class.java)
        arr.toMutableList()
    } catch (e: Exception) {
        logger.error("Failed to serialize value '$value'", e)
        null
    }
}
