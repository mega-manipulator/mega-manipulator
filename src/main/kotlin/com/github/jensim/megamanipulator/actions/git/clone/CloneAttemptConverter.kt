package com.github.jensim.megamanipulator.actions.git.clone

import com.github.jensim.megamanipulator.settings.SerializationHolder
import com.intellij.util.xmlb.Converter
import org.slf4j.LoggerFactory

class CloneAttemptConverter : Converter<MutableList<CloneAttempt>>() {

    companion object {
        private val logger = LoggerFactory.getLogger(CloneAttempt::class.java)
    }

    override fun toString(value: MutableList<CloneAttempt>): String? {
        return SerializationHolder.objectMapper.writeValueAsString(value)
    }

    override fun fromString(value: String): MutableList<CloneAttempt>? = try {
        val arr = SerializationHolder.objectMapper.readValue(value, Array<CloneAttempt>::class.java)
        arr.toMutableList()
    } catch (e: Exception) {
        logger.error("Failed to serialize value '$value'", e)
        null
    }
}
