package com.github.jensim.megamanipulatior.settings

import com.fasterxml.jackson.module.kotlin.readValue
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test

internal class SettingsFileOperatorTest {

    @Test
    fun serializeDeserialize() {
        // given
        val testData = MegaManipulatorSettings(
            null,
            searchHostSettings = mapOf(
                "sg" to SearchHostSettingsWrapper(
                    type = SearchHostType.SOURCEGRAPH,
                    settings = SourceGraphSettings(
                        baseUrl = "https://sourcegraph.example.com",
                        httpsOverride = null,
                        authMethod = AuthMethod.USERNAME_PASSWORD,
                        username = "null",
                    ),
                    codeHostSettings = mapOf(
                        "bb" to CodeHostSettingsWrapper(
                            type = CodeHostType.BITBUCKET_SERVER,
                            BitBucketSettings(
                                baseUrl = "https://bitbucket.example.com",
                                clonePattern = "ssh://git@bitbucket.example.com/{project}/{repo}.git",
                                httpsOverride = null,
                                authMethod = AuthMethod.TOKEN,
                                username = null,
                            )
                        )
                    )
                ),
            ),
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
            null,
            searchHostSettings = mapOf(
                "sg" to SearchHostSettingsWrapper(
                    type = SearchHostType.SOURCEGRAPH,
                    settings = SourceGraphSettings(
                        baseUrl = "https://sourcegraph.example.com",
                        httpsOverride = null,
                        authMethod = AuthMethod.USERNAME_PASSWORD,
                        username = null
                    ),
                    codeHostSettings = mapOf()
                ),
            ),
        )
        // when
        val yaml = SettingsFileOperator.objectMapper.writeValueAsString(testData)
        val deserialized: MegaManipulatorSettings = SettingsFileOperator.objectMapper.readValue(yaml)

        // then
        assertEquals(deserialized, testData)
        println(yaml)
    }
}
