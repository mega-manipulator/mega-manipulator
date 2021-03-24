package com.github.jensim.megamanipulator.settings

import com.fasterxml.jackson.module.kotlin.readValue
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SettingsFileOperatorTest {

    @Test
    fun serializeDeserialize() {
        // given
        val testData = MegaManipulatorSettings(
            defaultHttpsOverride = null,
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
                                forkSetting = ForkSetting.PLAIN_BRANCH,
                                forkRepoPrefix = null,
                            )
                        )
                    )
                ),
            ),
        )

        // when
        val yaml = SerializationHolder.yamlObjectMapper.writeValueAsString(testData)
        val deserialized: MegaManipulatorSettings = SerializationHolder.yamlObjectMapper.readValue(yaml)

        // then
        assertEquals(deserialized, testData)
        println(yaml)
    }

    @Test
    fun failIfTooFewEntries() {
        Assertions.assertThrows(IllegalArgumentException::class.java) {
            MegaManipulatorSettings(
                    defaultHttpsOverride = null,
                    searchHostSettings = mapOf(
                            "sg" to SearchHostSettingsWrapper(
                                    type = SearchHostType.SOURCEGRAPH,
                                    settings = SourceGraphSettings(
                                            baseUrl = "https://sourcegraph.example.com",
                                            httpsOverride = null,
                                            authMethod = AuthMethod.USERNAME_PASSWORD,
                                            username = "null"
                                    ),
                                    codeHostSettings = mapOf()
                            ),
                    ),
            )
        }
    }
}
