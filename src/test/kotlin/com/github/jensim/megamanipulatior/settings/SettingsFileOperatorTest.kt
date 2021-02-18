package com.github.jensim.megamanipulatior.settings

import com.fasterxml.jackson.module.kotlin.readValue
import javax.validation.Validation
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test

internal class SettingsFileOperatorTest {

    @Test
    fun serializeDeserialize() {
        // given
        val testData = MegaManipulatorSettings(
            sourceGraphSettings = SourceGraphSettings(baseUrl = "https://sourcegraph.example.com"),
            codeHostSettings = listOf(
                CodeHostSettingsWrapper(
                    type = CodeHostType.BITBUCKET_SERVER,
                    BitBucketSettings(
                        baseUrl = "https://bitbucket.example.com"
                    )
                )
            )
        )
        // when
        val yaml = SettingsFileOperator.objectMapper.writeValueAsString(testData)
        val deserialized: MegaManipulatorSettings = SettingsFileOperator.objectMapper.readValue(yaml)

        // then
        assertEquals(deserialized, testData)
        println(yaml)
    }

    @Test
    fun failIfTooFewEntries() {
        // given
        val testData = MegaManipulatorSettings(
            sourceGraphSettings = SourceGraphSettings(baseUrl = "https://sourcegraph.example.com"),
            codeHostSettings = listOf()
        )
        // when
        val yaml = SettingsFileOperator.objectMapper.writeValueAsString(testData)
        val deserialized: MegaManipulatorSettings = SettingsFileOperator.objectMapper.readValue(yaml)
        Validation.buildDefaultValidatorFactory().validator.validate(deserialized)

        // then
        assertEquals(deserialized, testData)
        println(yaml)
    }
}
